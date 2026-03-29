package eternal.future.tefmanager.ui.model

import kotlinx.serialization.Serializable

/*******************************************************************************
 * TEFManager - ModItem
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
 * Created: 2026/3/22
 *******************************************************************************/

@Serializable
data class ModItem(
    // 基础信息
    val pkgId: String = "",
    val name: String = "",
    val author: String = "",
    val version: String = "",
    val versionCode: Int = 0,

    // 描述信息
    val brieflyDescribe: String = "",      // 简短描述（显示在卡片上）
    val description: String = "",          // 详细描述

    // 特征信息
    val features: List<ModFeature> = listOf(), // Mod特征
    val sizeCategory: ModSizeCategory = ModSizeCategory.TINY, // 规模分类

    // 兼容性信息
    val targetGameVersion: String = "",    // 目标游戏版本
    val minGameVersion: String = "",       // 最低支持的游戏版本
    val maxGameVersion: String = "",       // 最高支持的游戏版本
    val support: PlatformSupport = PlatformSupport(),

    // 依赖关系
    val dependence: List<Dependence> = listOf(),
    val conflicts: List<String> = listOf(), // 冲突的Mod ID列表

    // 元数据
    val detailsURL: String = "",           // 详细信息URL

    // 状态标记
    val stableVerified: Boolean = false,         // 是否已验证稳定
    val experimental: Boolean = false,     // 是否实验性版本
    val deprecated: Boolean = false,       // 是否已弃用
    val hasExtendedContent: Boolean = false, // 是否有扩展内容

    val globalConfig: GlobalConfig = GlobalConfig.empty
) {

    @Serializable
    enum class ModFeature {
        // 功能性特征
        ASSISTANCE,           // 辅助型Mod（小地图、坐标显示、自动合成等）
        QUALITY_OF_LIFE,      // 生活质量改善
        UTILITY,              // 实用工具
        ACCESSIBILITY,        // 辅助功能
        VISUAL_ENHANCEMENT,   // 视觉增强
        UI_IMPROVEMENT,       // UI改进

        // 内容性特征
        NEW_CONTENT,          // 新增内容（物品/方块/生物）
        CONTENT_EXPANSION,    // 内容扩展
        OVERHAUL,             // 全面重制
        MECHANICS_CHANGE,     // 机制修改
        DIFFICULTY_MOD,       // 难度调整
        PROGRESSION_SYSTEM,   // 进度系统

        // 玩法特征
        ADVENTURE,            // 冒险/探索
        BUILDING,             // 建筑/建造
        TECHNOLOGY,           // 科技/自动化
        MAGIC,                // 魔法/神秘
        FANTASY,              // 幻想/魔幻
        SCI_FI,               // 科幻/未来
        REALISM,              // 写实/真实
        DECORATION,           // 装饰/美化

        // 多人特征
        MULTIPLAYER_ENHANCED, // 多人游戏增强
        SERVER_MANAGEMENT,    // 服务器管理
        COMMUNICATION;        // 通信/聊天

        fun getDisplayText(): String = when(this) {
            ASSISTANCE -> "辅助型"
            QUALITY_OF_LIFE -> "生活改善"
            UTILITY -> "实用工具"
            ACCESSIBILITY -> "辅助功能"
            VISUAL_ENHANCEMENT -> "视觉增强"
            UI_IMPROVEMENT -> "UI改进"

            NEW_CONTENT -> "新增内容"
            CONTENT_EXPANSION -> "内容扩展"
            OVERHAUL -> "全面重制"
            MECHANICS_CHANGE -> "机制修改"
            DIFFICULTY_MOD -> "难度调整"
            PROGRESSION_SYSTEM -> "进度系统"

            ADVENTURE -> "冒险/探索"
            BUILDING -> "建筑/建造"
            TECHNOLOGY -> "科技自动化"
            MAGIC -> "魔法/神秘"
            FANTASY -> "幻想/魔幻"
            SCI_FI -> "科幻/未来"
            REALISM -> "写实/真实"
            DECORATION -> "装饰/美化"

            MULTIPLAYER_ENHANCED -> "多人增强"
            SERVER_MANAGEMENT -> "服务器管理"
            COMMUNICATION -> "通信聊天"
        }
    }

    @Serializable
    enum class ModSizeCategory {
        TINY,     // 微型Mod（< 10KB）
        SMALL,    // 小型Mod（< 1MB）
        MEDIUM,   // 中型Mod（< 10MB）
        LARGE,    // 大型Mod（< 100MB）
        HUGE;     // 巨型Mod（>= 100MB）

        fun getDisplayText(): String = when(this) {
            TINY -> "微型"
            SMALL -> "小型"
            MEDIUM -> "中型"
            LARGE -> "大型"
            HUGE -> "巨型"
        }
    }
}