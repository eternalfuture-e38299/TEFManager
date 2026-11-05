/*******************************************************************************
 * TEFManager - Zygisk module
 * Copyright (C) 2026 eternalfuture-e38299
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU Affero General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the
 * GNU Affero General Public License for more details.
 *
 * You should have received a copy of the GNU Affero General Public License
 * along with this program. If not, see <https://www.gnu.org/licenses/>.
 *
 * Author: eternalfuture-e38299
 * GitHub: https://github.com/eternalfuture-e38299
 *******************************************************************************/

#include <android/log.h>
#include <fcntl.h>
#include <jni.h>
#include <pthread.h>
#include <sys/stat.h>
#include <sys/types.h>
#include <sys/xattr.h>
#include <unistd.h>

#include <cerrno>
#include <android/dlext.h>
#include <dlfcn.h>
#include <cstdio>
#include <cstring>
#include <string>

#include "zygisk.hpp"

#define LOG_TAG "TEFZygisk"
#define LOGI(...) __android_log_print(ANDROID_LOG_INFO, LOG_TAG, __VA_ARGS__)
#define LOGW(...) __android_log_print(ANDROID_LOG_WARN, LOG_TAG, __VA_ARGS__)
#define LOGE(...) __android_log_print(ANDROID_LOG_ERROR, LOG_TAG, __VA_ARGS__)

namespace {

    constexpr const char *kTefManagerPackage = "eternal.future.tefmanager";
    constexpr const char *kTargetPackages[] = {
            "com.and.games505.Terraria",
            "com.and.games505.TerrariaPaid",
    };

    constexpr const char *kKernelSubDir = "tefkernel";
    constexpr const char *kKernelPrefix = "libtefkernel.android.";
    constexpr const char *kKernelSuffix = ".so";
    constexpr const char *kCopiedKernelName = "libtefkernel.zygisk.so";
    constexpr const char *kWorkingDirConfigName = "tefkernel_working_dir";

    struct IpcRequest {
        int32_t uid;
        char data_dir[256];
    };

    struct IpcResponse {
        int32_t ok;
        char working_dir[512];
    };

    JavaVM *g_vm = nullptr;

    bool read_all(const int fd, void *buf, const size_t len) {
        auto *p = static_cast<unsigned char *>(buf);
        size_t left = len;
        while (left > 0) {
            const ssize_t n = ::read(fd, p, left);
            if (n <= 0) return false;
            p += n;
            left -= static_cast<size_t>(n);
        }
        return true;
    }

    bool write_all(const int fd, const void *buf, const size_t len) {
        const auto *p = static_cast<const unsigned char *>(buf);
        size_t left = len;
        while (left > 0) {
            const ssize_t n = ::write(fd, p, left);
            if (n <= 0) return false;
            p += n;
            left -= static_cast<size_t>(n);
        }
        return true;
    }

    std::string abi_name() {
#if defined(__aarch64__)
        return "arm64-v8a";
#elif defined(__arm__)
        return "armeabi-v7a";
#elif defined(__x86_64__)
        return "x86_64";
#elif defined(__i386__)
        return "x86";
#else
        return "unknown";
#endif
    }

    /// 以游戏数据目录为基准，安全地创建/覆盖内核文件。
    /// 关键：中间目录和最终文件都用 O_NOFOLLOW，禁止跟随攻击者放置的符号链接；
    /// 内核源文件也拒绝符号链接，并必须是普通文件。
    bool copy_kernel_to_app(const std::string &src, const std::string &dataDir, const int uid) {
        const int in = ::open(src.c_str(), O_RDONLY | O_CLOEXEC | O_NOFOLLOW);
        if (in < 0) {
            LOGE("open kernel source failed: %s (%s)", src.c_str(), strerror(errno));
            return false;
        }
        struct stat st{};
        if (::fstat(in, &st) != 0 || !S_ISREG(st.st_mode) || st.st_size <= 0) {
            LOGE("kernel source is not a regular file: %s", src.c_str());
            ::close(in);
            return false;
        }

        const int dataFd = ::open(dataDir.c_str(), O_RDONLY | O_DIRECTORY | O_CLOEXEC);
        if (dataFd < 0) {
            LOGE("open app data dir failed: %s (%s)", dataDir.c_str(), strerror(errno));
            ::close(in);
            return false;
        }

        int filesFd = ::openat(dataFd, "files",
                               O_RDONLY | O_DIRECTORY | O_CLOEXEC | O_NOFOLLOW);
        if (filesFd < 0 && errno == ENOENT) {
            if (::mkdirat(dataFd, "files", 0700) != 0 && errno != EEXIST) {
                LOGE("mkdir app files failed: %s", strerror(errno));
            }
            filesFd = ::openat(dataFd, "files",
                               O_RDONLY | O_DIRECTORY | O_CLOEXEC | O_NOFOLLOW);
        }
        if (filesFd < 0) {
            LOGE("app files dir is not a safe directory: %s (%s)",
                 dataDir.c_str(), strerror(errno));
            ::close(dataFd);
            ::close(in);
            return false;
        }

        const int out = ::openat(filesFd, kCopiedKernelName,
                                 O_WRONLY | O_CREAT | O_TRUNC | O_CLOEXEC | O_NOFOLLOW, 0700);
        if (out < 0) {
            LOGE("open kernel destination failed: %s", strerror(errno));
            ::close(filesFd);
            ::close(dataFd);
            ::close(in);
            return false;
        }

        char buf[64 * 1024];
        bool ok = true;
        ssize_t n;
        while ((n = ::read(in, buf, sizeof(buf))) > 0) {
            if (!write_all(out, buf, static_cast<size_t>(n))) {
                ok = false;
                break;
            }
        }
        if (n < 0) ok = false;
        ::close(in);

        if (ok) {
            if (::fchown(out, uid, uid) != 0) LOGW("fchown kernel failed: %s", strerror(errno));
            if (::fchmod(out, 0700) != 0) LOGW("fchmod kernel failed: %s", strerror(errno));
            const char label[] = "u:object_r:app_data_file:s0";
            if (::fsetxattr(out, "security.selinux", label, sizeof(label) - 1, 0) != 0) {
                LOGW("fsetxattr kernel failed: %s", strerror(errno));
            }
        } else {
            LOGE("copy kernel failed");
        }
        ::close(out);
        ::close(filesFd);
        ::close(dataFd);
        return ok;
    }

    bool write_text(const std::string &path, const std::string &text) {
        const int fd = ::open(path.c_str(), O_WRONLY | O_CREAT | O_TRUNC | O_CLOEXEC, 0600);
        if (fd < 0) return false;
        const bool ok = write_all(fd, text.data(), text.size());
        ::close(fd);
        return ok;
    }

    void set_app_data_label(const std::string &path) {
        const char label[] = "u:object_r:app_data_file:s0";
        if (::setxattr(path.c_str(), "security.selinux", label, sizeof(label) - 1, 0) == 0)
            return;
        LOGW("setxattr(security.selinux) failed for %s: %s", path.c_str(), strerror(errno));

        // 部分内核/文件系统不允许直接写 xattr，退回 libselinux 的路径 restorecon。
        using Restorecon = int (*)(const char *, unsigned int);
        void *selinux = ::dlopen("libselinux.so", RTLD_NOW);
        if (!selinux) return;
        auto restorecon = reinterpret_cast<Restorecon>(
                ::dlsym(selinux, "selinux_android_restorecon"));
        if (restorecon && restorecon(path.c_str(), 0) != 0) {
            LOGW("restorecon failed for %s: %s", path.c_str(), strerror(errno));
        }
        ::dlclose(selinux);
    }

    std::string external_files_dir(const int userId) {
        return "/data/media/" + std::to_string(userId) +
               "/Android/data/" + kTefManagerPackage + "/files";
    }

    std::string visible_external_files_dir(const int userId) {
        return "/storage/emulated/" + std::to_string(userId) +
               "/Android/data/" + kTefManagerPackage + "/files";
    }

    /// 根 companion：把 TEFManager 下载的内核复制进目标游戏私有目录。
    void companion_handler(const int fd) {
        IpcRequest req{};
        if (!read_all(fd, &req, sizeof(req))) return;

        req.data_dir[sizeof(req.data_dir) - 1] = '\0';
        const int userId = req.uid / 100000;
        const std::string abi = abi_name();
        const std::string ext = external_files_dir(userId);
        const std::string src = ext + "/" + kKernelSubDir + "/" + kKernelPrefix + abi + kKernelSuffix;
        // 首次启动时 files 目录可能还没被 App 创建；companion 以 root 补建并交给目标 UID。
        const std::string filesDir = std::string(req.data_dir) + "/files";
        if (::mkdir(filesDir.c_str(), 0700) == 0) {
            ::chown(filesDir.c_str(), req.uid, req.uid);
            set_app_data_label(filesDir);
        }

        IpcResponse resp{};
        std::snprintf(resp.working_dir, sizeof(resp.working_dir), "%s",
                      visible_external_files_dir(userId).c_str());

        struct stat st{};
        if (::stat(src.c_str(), &st) != 0 || st.st_size <= 0) {
            LOGE("kernel not found: %s (%s)", src.c_str(), strerror(errno));
            resp.ok = 0;
            write_all(fd, &resp, sizeof(resp));
            return;
        }

        if (!copy_kernel_to_app(src, req.data_dir, req.uid)) {
            resp.ok = 0;
            write_all(fd, &resp, sizeof(resp));
            return;
        }

        LOGI("kernel copied to %s/files/%s (uid=%d, abi=%s, %lld bytes)", req.data_dir,
             kCopiedKernelName, req.uid, abi.c_str(), static_cast<long long>(st.st_size));
        resp.ok = 1;
        write_all(fd, &resp, sizeof(resp));
    }

    struct LoadJob {
        std::string dataDir;
        std::string workingDir;
    };

    jobject current_application(JNIEnv *env) {
        jclass at = env->FindClass("android/app/ActivityThread");
        if (!at) {
            env->ExceptionClear();
            return nullptr;
        }
        jmethodID current = env->GetStaticMethodID(at, "currentActivityThread",
                                                   "()Landroid/app/ActivityThread;");
        if (!current) {
            env->ExceptionClear();
            env->DeleteLocalRef(at);
            return nullptr;
        }
        jobject thread = env->CallStaticObjectMethod(at, current);
        if (env->ExceptionCheck() || !thread) {
            env->ExceptionClear();
            if (thread) env->DeleteLocalRef(thread);
            env->DeleteLocalRef(at);
            return nullptr;
        }
        jmethodID getApp = env->GetMethodID(at, "getApplication", "()Landroid/app/Application;");
        if (!getApp) {
            env->ExceptionClear();
            if (thread) env->DeleteLocalRef(thread);
            env->DeleteLocalRef(at);
            return nullptr;
        }
        jobject app = env->CallObjectMethod(thread, getApp);
        if (env->ExceptionCheck()) {
            env->ExceptionClear();
            app = nullptr;
        }
        if (thread) env->DeleteLocalRef(thread);
        env->DeleteLocalRef(at);
        return app;
    }

    /// 已修补 APK 自带 Tefloader，会通过自己的方式加载内核，这里必须让路。
    bool has_patched_tefloader(JNIEnv *env, jobject application) {
        jclass appClass = env->GetObjectClass(application);
        jmethodID getClassLoader = env->GetMethodID(appClass, "getClassLoader",
                                                    "()Ljava/lang/ClassLoader;");
        if (!getClassLoader) {
            env->ExceptionClear();
            env->DeleteLocalRef(appClass);
            return false;
        }
        jobject loader = env->CallObjectMethod(application, getClassLoader);
        if (env->ExceptionCheck() || !loader) {
            env->ExceptionClear();
            env->DeleteLocalRef(appClass);
            return false;
        }
        jclass loaderClass = env->FindClass("java/lang/ClassLoader");
        jmethodID loadClass = env->GetMethodID(loaderClass, "loadClass",
                                               "(Ljava/lang/String;)Ljava/lang/Class;");
        jstring name = env->NewStringUTF("eternal.future.tefkernel.Tefloader");
        jobject found = loadClass ? env->CallObjectMethod(loader, loadClass, name) : nullptr;
        const bool patched = !env->ExceptionCheck() && found != nullptr;
        env->ExceptionClear();
        if (found) env->DeleteLocalRef(found);
        if (name) env->DeleteLocalRef(name);
        if (loaderClass) env->DeleteLocalRef(loaderClass);
        if (loader) env->DeleteLocalRef(loader);
        env->DeleteLocalRef(appClass);
        return patched;
    }

    /// 原生线程里 System.load 的 caller class 为 null，Runtime.load0 会 NPE。
    /// 因此失败后退回 android_dlopen_ext，并显式使用 App 的 classloader namespace。
    bool native_load(const std::string &path) {
        using GetNamespace = android_namespace_t *(*)(const char *);
        auto getNamespace = reinterpret_cast<GetNamespace>(
                ::dlsym(RTLD_DEFAULT, "android_get_exported_namespace"));
        if (getNamespace) {
            android_namespace_t *ns = getNamespace("classloader-namespace");
            if (ns) {
                android_dlextinfo info{};
                info.flags = ANDROID_DLEXT_USE_NAMESPACE;
                info.library_namespace = ns;
                void *handle = ::android_dlopen_ext(path.c_str(), RTLD_NOW, &info);
                if (handle) {
                    LOGI("TEFKernel loaded with classloader namespace");
                    return true;
                }
                LOGE("android_dlopen_ext(classloader) failed: %s", ::dlerror());
            }
        }
        void *handle = ::dlopen(path.c_str(), RTLD_NOW | RTLD_GLOBAL);
        if (handle) {
            LOGI("TEFKernel loaded with dlopen");
            return true;
        }
        LOGE("dlopen failed: %s", ::dlerror());
        return false;
    }

    /// 使用 Runtime.load0(Class, String) 显式传入 Application 的 Class。
    /// 这样 Runtime 能拿到正确的 App ClassLoader，加载到 App 的 linker namespace，
    /// 后续 xdl_open("libil2cpp.so") 才能找到游戏库。
    /// 直接 System.load 在原生线程里 caller class 为 null，会 NPE。
    bool java_load(JNIEnv *env, jobject application, const std::string &path) {
        jclass runtime = env->FindClass("java/lang/Runtime");
        if (!runtime) return false;
        jmethodID load0 = env->GetStaticMethodID(
                runtime, "load0", "(Ljava/lang/Class;Ljava/lang/String;)V");
        if (!load0) {
            env->ExceptionClear();
            env->DeleteLocalRef(runtime);
            return false;
        }
        jclass appClass = env->GetObjectClass(application);
        jstring arg = env->NewStringUTF(path.c_str());
        env->CallStaticVoidMethod(runtime, load0, appClass, arg);
        bool ok = !env->ExceptionCheck();
        if (!ok) {
            LOGE("Runtime.load0 failed for %s", path.c_str());
            env->ExceptionDescribe();
            env->ExceptionClear();
        }
        if (arg) env->DeleteLocalRef(arg);
        if (appClass) env->DeleteLocalRef(appClass);
        env->DeleteLocalRef(runtime);
        return ok;
    }

    void *load_thread(void *opaque) {
        auto *job = static_cast<LoadJob *>(opaque);
        JNIEnv *env = nullptr;
        if (!g_vm || g_vm->AttachCurrentThread(&env, nullptr) != JNI_OK || !env) {
            delete job;
            return nullptr;
        }

        jobject app = nullptr;
        for (int i = 0; i < 200 && !app; ++i) {
            app = current_application(env);
            if (!app) ::usleep(50 * 1000);
        }
        if (!app) {
            LOGE("Application not ready, TEFKernel injection skipped");
            g_vm->DetachCurrentThread();
            delete job;
            return nullptr;
        }

        if (has_patched_tefloader(env, app)) {
            LOGI("patched Tefloader detected, Zygisk injection skipped");
            env->DeleteLocalRef(app);
            g_vm->DetachCurrentThread();
            delete job;
            return nullptr;
        }

        const std::string cfg = job->dataDir + "/files/" + kWorkingDirConfigName;
        const std::string so = job->dataDir + "/files/" + kCopiedKernelName;
        if (!write_text(cfg, job->workingDir)) {
            LOGE("failed to write working dir config: %s", cfg.c_str());
        }
        LOGI("loading TEFKernel: %s (working dir %s)", so.c_str(), job->workingDir.c_str());
        bool loaded = java_load(env, app, so);
        if (!loaded) {
            LOGW("Runtime.load0 failed, falling back to native loader");
            loaded = native_load(so);
        }
        if (!loaded) LOGE("TEFKernel load failed");
        ::unlink(cfg.c_str());

        env->DeleteLocalRef(app);
        g_vm->DetachCurrentThread();
        delete job;
        return nullptr;
    }

    class TefZygisk : public zygisk::ModuleBase {
    public:
        void onLoad(zygisk::Api *zygiskApi, JNIEnv *jniEnv) override {
            api = zygiskApi;
            env = jniEnv;
            env->GetJavaVM(&g_vm);
        }

        void preAppSpecialize(zygisk::AppSpecializeArgs *args) override {
            if (!env || !args->nice_name) return;

            const char *nice = env->GetStringUTFChars(args->nice_name, nullptr);
            const bool target = nice && is_target(nice);
            env->ReleaseStringUTFChars(args->nice_name, nice);
            if (!target) return;

            if (!args->app_data_dir) return;
            const char *dir = env->GetStringUTFChars(args->app_data_dir, nullptr);
            if (!dir) return;
            dataDir = dir;
            env->ReleaseStringUTFChars(args->app_data_dir, dir);

            LOGI("target process detected, data dir=%s", dataDir.c_str());

            const int fd = api->connectCompanion();
            if (fd < 0) {
                LOGE("connectCompanion failed");
                return;
            }

            IpcRequest req{};
            req.uid = args->uid;
            std::snprintf(req.data_dir, sizeof(req.data_dir), "%s", dataDir.c_str());
            IpcResponse resp{};
            const bool sent = write_all(fd, &req, sizeof(req));
            const bool got = sent && read_all(fd, &resp, sizeof(resp));
            ::close(fd);

            if (!got || resp.ok != 1) {
                LOGE("root companion failed to prepare kernel");
                return;
            }
            workingDir = resp.working_dir;
            kernelReady = true;
        }

        void postAppSpecialize(const zygisk::AppSpecializeArgs *) override {
            if (!kernelReady) return;
            auto *job = new LoadJob{dataDir, workingDir};
            pthread_t thread;
            if (pthread_create(&thread, nullptr, load_thread, job) != 0) {
                LOGE("failed to create loader thread");
                delete job;
                return;
            }
            pthread_detach(thread);
        }

    private:
        static bool is_target(const char *name) {
            for (const char *pkg : kTargetPackages) {
                if (std::strcmp(name, pkg) == 0) return true;
            }
            return false;
        }

        zygisk::Api *api = nullptr;
        JNIEnv *env = nullptr;
        std::string dataDir;
        std::string workingDir;
        bool kernelReady = false;
    };

} // namespace

REGISTER_ZYGISK_MODULE(TefZygisk)
REGISTER_ZYGISK_COMPANION(companion_handler)
