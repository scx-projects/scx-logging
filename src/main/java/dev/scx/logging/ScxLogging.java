package dev.scx.logging;

import dev.scx.logging.recorder.ConsoleRecorder;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicLong;
import java.util.regex.Pattern;

import static java.lang.System.Logger.Level.ERROR;

/// ScxLogging
///
/// # 配置模型设计说明
///
/// ScxLogging 采用一种明确的配置模型:
///
/// ```text
/// rootConfig + 有序声明规则
/// ```
///
/// 这份说明用于固定 ScxLogging 的配置语义, 以及解释为什么没有采用其他日志框架中常见的设计方式.
///
/// 日志配置表面上看只是:
///
/// ```text
/// 给某个 logger name 设置日志级别.
/// ```
///
/// 但一旦同时支持:
///
/// ```text
/// 1. root 默认配置
/// 2. 精确 logger name 配置
/// 3. 正则 logger name 配置
/// 4. 运行期重新配置
/// 5. 懒创建 logger
/// ```
///
/// 配置语义就不再只是简单的 Map 设置值.
///
/// ScxLogging 的设计目标不是复刻某个传统日志框架, 而是提供一套小而统一、可以通过声明顺序推导最终结果的配置模型.
///
/// 本设计的核心原则是:
///
/// ```text
/// matcher 只负责判断是否匹配.
/// order   只负责决定谁覆盖谁.
/// config  只负责声明覆盖哪些字段.
/// ```
///
/// 只要这三件事保持分离, 配置模型就是确定、可解释、可扩展的.
///
/// ---
///
/// ## 1. 核心心智模型:声明, 而不是编辑
///
/// ScxLogging 的配置 API 采用"声明语义", 不是"编辑语义".
///
/// 调用:
///
/// ```java
/// config(key, config)
/// ```
///
/// 它的含义不是:
///
/// ```text
/// 找到内部配置表里 key 对应的那一行, 然后编辑它.
/// ```
///
/// 而是:
///
/// ```text
/// 现在声明 key 对应的配置规则是 config.
/// ```
///
/// 这点非常重要.
///
/// 如果把 config(...) 理解成"编辑内部配置表", 用户就必须理解内部状态:
///
/// ```text
/// 这个 key 之前是否存在？
/// 如果存在, 它原来在第几条？
/// 这次调用是修改旧位置, 还是移动到最后？
/// 如果保留旧位置, 为什么刚 config 的内容没有最新优先级？
/// 如果移动到最后, 为什么一次修改又改变了优先级？
/// ```
///
/// 这些问题都会把用户拖进内部实现.
///
/// ScxLogging 不希望用户为了理解一次 config(...) 调用,
/// 还必须知道内部之前是否已经存在同 key 的配置.
///
/// 因此, ScxLogging 将 config(...) 统一理解为:
///
/// ```text
/// 发出一次新的配置声明.
/// ```
///
/// 无论 key 之前是否存在, 本次 config(...) 都表示:
///
/// ```text
/// 让这个配置现在生效.
/// ```
///
/// 所以:
///
/// ```text
/// 如果 key 不存在, 创建一条新声明, 并放到声明顺序末尾.
/// 如果 key 已存在, 旧声明失效, 新声明成为当前声明, 并放到声明顺序末尾.
/// ```
///
/// 同一个 key 只保留当前有效声明.
///
/// 这使用户不需要理解"新增"和"修改"的内部差别.
///
/// 对用户来说, config(...) 永远只有一个含义:
///
/// ```text
/// 现在声明这条规则.
/// ```
///
/// 既然是现在声明, 它就拥有最新的覆盖优先级.
///
/// ---
///
/// ## 2. rootConfig 是什么？
///
/// rootConfig 是基础配置.
///
/// 它是整个配置系统中唯一的特殊配置.
///
/// rootConfig:
///
/// ```text
/// 1. 永远最先应用.
/// 2. 不参与 matcher 匹配.
/// 3. 不是普通 override rule.
/// 4. 没有声明顺序问题.
/// ```
///
/// rootConfig 的职责是提供完整默认值, 保证任意 loggerName 最终都能解析出
/// 一份完整可用的配置.
///
/// 因此 rootConfig 必须是完整配置:
///
/// ```text
/// rootConfig.level      不允许为 null
/// rootConfig.stackTrace 不允许为 null
/// rootConfig.recorders  不允许为 null
/// ```
///
/// 普通规则可以是局部声明.
/// rootConfig 不应该是局部声明.
///
/// 解析任意 loggerName 时, 逻辑上都从 rootConfig 开始:
///
/// ```text
/// result = rootConfig
/// ```
///
/// 然后再按顺序应用后续所有匹配的声明规则.
///
/// ---
///
/// ## 3. ScxLoggerConfig 是什么？
///
/// ScxLoggerConfig 是一个不可变的配置声明值.
///
/// 它不是一个"活的配置句柄".
/// 它不属于某个 ScxLogging 内部 entry.
/// 它不会在修改时通知 ScxLogging.
/// 它也不应该被原地修改.
/// 事实上, 它根本不能被修改.
/// 用户可以自由创建、保存、复用、比较、序列化 ScxLoggerConfig.
///
/// ScxLoggerConfig 应被理解为:
///
/// ```text
/// 一次配置声明中, 声明了哪些字段, 以及这些字段的值是什么.
/// ```
///
/// 在普通 override rule 中, ScxLoggerConfig 可以是局部声明.
///
/// 例如:
///
/// - `level == null` -> 本次声明没有声明 level, 因此不覆盖之前解析得到的 level.
/// - `stackTrace == null` -> 本次声明没有声明 stackTrace, 因此不覆盖之前解析得到的 stackTrace.
/// - `recorders == null` -> 本次声明没有声明 recorders, 因此不覆盖之前解析得到的 recorders.
/// - `recorders == 空集合` -> 本次声明明确声明 recorders 为空, 因此会覆盖之前的 recorders.
///
/// 这里, null 和空集合必须区分:
///
/// ```text
/// null      = 不声明, 不覆盖之前的值.
/// emptyList = 明确声明为空, 覆盖之前的值.
/// non-null  = 明确声明该字段, 覆盖之前的值.
/// ```
///
/// 这使 ScxLoggerConfig 可以表达局部覆盖,
/// 而不需要在 ScxLogging 核心 API 中引入额外的 patch/update 语义.
///
/// ---
///
/// ## 4. 非 root 配置都是 override rule
///
/// 除 rootConfig 外, 所有配置都是 override rule.
///
/// 一个 override rule 由两部分组成:
///
/// ```text
/// 1. matcher
/// 2. partial ScxLoggerConfig
/// ```
///
/// matcher 负责判断这条规则是否适用于某个 loggerName.
///
/// ScxLoggerConfig 负责声明这条规则要覆盖哪些字段.
///
/// 声明顺序负责决定多条匹配规则之间谁覆盖谁.
///
/// 当前支持的 matcher 有:
///
/// ```text
/// 1. 精确 logger name
/// 2. 正则表达式
/// ```
///
/// 未来也可以加入其他 matcher 类型.
///
/// 重要的是:
///
/// ```text
/// exact name 和 regex pattern 都只是 matcher 类型.
/// 它们不是优先级层.
/// ```
///
/// exact name 的特殊性仅在于:
///
/// ```text
/// 它只匹配一个 loggerName.
/// ```
///
/// regex pattern 的特殊性仅在于:
///
/// ```text
/// 它可以匹配多个 loggerName.
/// ```
///
/// 一旦规则匹配成功, 它和其他匹配规则的覆盖关系只由声明顺序决定.
///
/// 也就是说:
///
/// ```text
/// matcher 只回答"是否适用".
/// order   只回答"谁覆盖谁".
/// config  只回答"覆盖哪些字段".
/// ```
///
/// 这三个职责不能混在一起.
///
/// ---
///
/// ## 5. 配置解析算法
///
/// 对某个 loggerName 解析配置时, 算法如下:
///
/// ```text
/// result = rootConfig
///
/// for each rule in declarationOrder:
///     if rule.matcher matches loggerName:
///         result = merge(result, rule.config)
///
/// return result
/// ```
///
/// merge 是字段级覆盖:
///
/// ```text
/// if rule.config.level != null:
///     result.level = rule.config.level
///
/// if rule.config.stackTrace != null:
///     result.stackTrace = rule.config.stackTrace
///
/// if rule.config.recorders != null:
///     result.recorders = rule.config.recorders
/// ```
///
/// 如果某字段为 null, 则表示该规则没有声明该字段, 因此保留之前的结果.
///
/// 所以, 后面的匹配规则不会整体替换之前结果.
/// 它只覆盖自己明确声明的字段.
///
/// 例如:
///
/// ```text
/// root:
///     level = ERROR
///     stackTrace = false
///     recorders = [console]
///
/// rule[0]:
///     matcher = regex com\.abc\..*
///     level = DEBUG
///
/// rule[1]:
///     matcher = name com.abc.Test
///     stackTrace = true
/// ```
///
/// 对于 loggerName:
///
/// ```text
/// com.abc.Test
/// ```
///
/// 最终结果是:
///
/// ```text
/// level = DEBUG
/// stackTrace = true
/// recorders = [console]
/// ```
///
/// 因为:
///
/// ```text
/// level 来自 rule[0]
/// stackTrace 来自 rule[1]
/// recorders 来自 root
/// ```
///
/// ---
///
/// ## 6. 为什么不采用 root < regex < exact？
///
/// 很多日志系统习惯采用类似这样的优先级模型:
///
/// ```text
/// root < package/category < exact logger
/// ```
///
/// 或者在支持正则时设计成:
///
/// ```text
/// root < regex < exact
/// ```
///
/// 也就是说:
///
/// ```text
/// 精确 logger name 配置永远高于正则配置.
/// ```
///
/// 这是一个合理模型.
/// 但 ScxLogging 没有采用它.
///
/// 原因不是 exact 优先级模型不能工作,
/// 而是它和正则规则的顺序模型结合后, 会产生两套不同的心智规则.
///
/// 正则之间通常无法可靠比较"谁更精确".
///
/// 例如:
///
/// ```text
/// com\.abc\..*
/// com\..*\.Test
/// ```
///
/// 哪一个更精确？
///
/// 对人来说也许可以根据业务语义猜测.
/// 但对日志库来说, 很难设计一个简单、稳定、可预测、通用的比较规则.
///
/// 因此, 多个正则之间最自然的覆盖规则是:
///
/// ```text
/// 后声明的正则覆盖先声明的正则.
/// ```
///
/// 但如果正则之间按声明顺序覆盖, 而 exact 又永远高于所有 regex,
/// 就会出现双重规则:
///
/// ```text
/// regex B 可以因为声明更晚而覆盖 regex A.
/// 但 regex B 即使声明更晚, 也不能覆盖 exact A.
/// ```
///
/// 这要求用户理解一个隐藏层级:
///
/// ```text
/// regex 配置处于一个有序规则层.
/// exact 配置处于一个更高的特殊层.
/// ```
///
/// 于是用户会问:
///
/// ```text
/// 为什么后声明的 regex 可以覆盖前面的 regex,
/// 却不能覆盖前面的 exact？
/// ```
///
/// 答案只能是:
///
/// ```text
/// 因为 exact 有隐藏的最高优先级.
/// ```
///
/// ScxLogging 故意避免这种隐藏优先级层.
///
/// 在 ScxLogging 中:
///
/// ```text
/// exact 之所以有用, 是因为它匹配得精确.
/// 不是因为它拥有秘密优先级.
/// ```
///
/// 如果用户希望某个 exact 最终生效, 就在更宽泛的规则之后声明它.
///
/// 如果用户希望某个 regex 临时覆盖一批 logger, 包括已有 exact 配置,
/// 就在最后声明这个 regex.
///
/// 这样, 配置文件或配置调用顺序本身就能解释最终结果.
///
/// ---
///
/// ## 7. 为什么选择有序声明规则？
///
/// 有序声明规则最接近以下直觉:
///
/// ```text
/// 前面的声明建立基础倾向.
/// 后面的声明修正或覆盖前面的声明.
/// ```
///
/// 例如:
///
/// ```text
/// root:
///     level = INFO
///
/// rules:
///     name  com.abc.Test      level = ERROR
///     regex com\.abc\..*      level = DEBUG
/// ```
///
/// 对于:
///
/// ```text
/// com.abc.Test
/// ```
///
/// 两条规则都匹配.
///
/// 因为 regex 规则声明得更晚, 所以最终 level 是:
///
/// ```text
/// DEBUG
/// ```
///
/// 如果用户希望 com.abc.Test 保持 ERROR, 就应该写成:
///
/// ```text
/// root:
///     level = INFO
///
/// rules:
///     regex com\.abc\..*      level = DEBUG
///     name  com.abc.Test      level = ERROR
/// ```
///
/// 此时最终 level 是:
///
/// ```text
/// ERROR
/// ```
///
/// 这里没有隐藏特例.
///
/// 核心规则只有一条:
///
/// ```text
/// root 最先应用.
/// 之后按声明顺序应用所有匹配规则.
/// 后声明的匹配规则覆盖先声明的同字段值.
/// ```
///
/// ---
///
/// ## 8. 有序声明模型的实际价值
///
/// 有序声明模型支持一个非常实用的运维和调试场景:
///
/// ```text
/// 临时把某个包下的日志全部打开到 DEBUG.
/// ```
///
/// 假设原配置中已有:
///
/// ```text
/// name com.abc.Test level = ERROR
/// ```
///
/// 调试时, 用户只需要追加一条:
///
/// ```text
/// regex com\.abc\..* level = DEBUG
/// ```
///
/// 在 ScxLogging 的有序声明模型中, 这条新规则会影响所有匹配的 logger,
/// 包括之前已经有 exact 配置的 logger.
///
/// 这符合"我现在追加一条调试规则, 让它现在生效"的用户意图.
///
/// 如果采用"exact 永远优先"的模型,
/// 这条 regex 调试规则无法影响已经存在 exact 配置的 logger.
///
/// 用户必须找到并删除或修改那些 exact 配置.
///
/// 这会降低运行期调试和追加覆盖的表达力.
///
/// ScxLogging 选择有序声明模型, 是因为它让 "追加一条后声明规则"
/// 成为一种统一、直接、可解释的覆盖方式.
///
/// ---
///
/// ## 9. config(...) 的最终语义
///
/// ScxLogging 中的 config(...) 是核心 API.
///
/// 它的语义必须非常明确:
///
/// ```text
/// config(key, config) 表示:现在声明 key 对应的配置规则.
/// ```
///
/// 如果 key 之前不存在:
///
/// ```text
/// 创建一条新规则, 放到声明顺序末尾.
/// ```
///
/// 如果 key 之前已经存在:
///
/// ```text
/// 旧规则失效.
/// 新规则成为该 key 的当前规则.
/// 新规则放到声明顺序末尾.
/// ```
///
/// 因此, 同一个 key 只保留当前有效规则.
/// 但是每次 config(...) 都是一次新的声明.
///
/// 这保证用户不需要知道内部之前有没有这条配置.
///
/// 无论是第一次配置, 还是重新配置, 用户的理解都是同一句话:
///
/// ```text
/// 让这个配置现在生效.
/// ```
///
/// 所以, 重新 config 一个已有 key 时, 移动到最后不是实现细节,
/// 而是声明语义的自然结果.
///
/// 因为:
///
/// ```text
/// 现在声明的规则, 就应该具有现在的覆盖优先级.
/// ```
///
/// ---
///
/// ## 10. 为什么不提供"局部更新且保持优先级"的语义？
///
/// ScxLogging 不把核心 API 设计成配置编辑器.
///
/// 因此, ScxLogging 核心层不提供类似以下语义:
///
/// ```text
/// 找到已有规则.
/// 修改其中某个字段.
/// 但保留这条规则原来的声明位置.
/// ```
///
/// 这种语义看似方便, 但它引入了另一套心智模型:
///
/// ```text
/// 内部有一张配置表.
/// 用户可以编辑其中一行.
/// 编辑时还要理解是否改变该行顺序.
/// ```
///
/// 一旦引入这种模型, 用户就必须理解:
///
/// ```text
/// config(...) 是重新声明, 还是编辑旧规则？
/// updateConfig(...) 是编辑旧规则, 还是产生新声明？
/// 局部更新为什么不拥有最新优先级？
/// 全量设置为什么拥有最新优先级？
/// 如果用户不知道旧规则存在, 结果为什么不同？
/// ```
///
/// 这会让 ScxLogging 的核心语义变复杂.
///
/// ScxLogging 的选择是:
///
/// ```text
/// config(...) 不是编辑旧规则.
/// config(...) 是发出当前声明.
/// ```
///
/// 所以不提供"就地编辑旧声明且保留旧优先级"的核心 API.
///
/// 如果用户想表达一个新的配置意图, 就调用 config(...).
/// 这个意图会作为新的声明生效.
///
/// 如果未来提供更细粒度的便捷 API, 例如:
///
/// ```java
/// configLevel(name, level)
/// configStackTrace(name, stackTrace)
/// configRecorders(name, recorders)
/// ```
///
/// 它们也不应该被理解为"编辑旧规则".
/// 它们仍然应该被理解为:
///
/// ```text
/// 现在声明 key 的某个字段.
/// ```
///
/// 因此它们也应该遵守同一原则:
///
/// ```text
/// 产生当前声明, 并具有最新覆盖优先级.
/// ```
///
/// 但第一版 ScxLogging 可以不提供这些便捷 API.
///
/// 只提供:
///
/// ```java
/// config(key, ScxLoggerConfig)
/// ```
///
/// 已经足以表达局部声明, 因为 ScxLoggerConfig 本身支持 null 字段.
///
/// ---
///
/// ## 11. 为什么不区分"全量设置"和"局部更新"的优先级？
///
/// 从用户意图看, 所谓"全量设置"和"局部更新"的本质是一样的:
///
/// ```text
/// 用户希望这个配置现在生效.
/// ```
///
/// 如果设计成:
///
/// ```text
/// 全量设置会移动到最后.
/// 局部更新不移动到最后.
/// ```
///
/// 那么用户就必须理解 API 内部对"设置"和"更新"的区分.
///
/// 但在 ScxLogging 的声明模型中, 这种区分没有必要.
///
/// 一次配置调用不应该因为它声明了一个字段还是三个字段,
/// 就拥有完全不同的优先级语义.
///
/// 更简单的规则是:
///
/// ```text
/// 只要公开 API 改变了某个 key 的配置声明, 它就是一次新的声明.
/// 新声明拥有最新优先级.
/// ```
///
/// 至于这个声明中哪些字段为 null, 哪些字段非 null,
/// 那是 merge 阶段的字段覆盖问题,
/// 不是优先级问题.
///
/// ---
///
/// ## 12. 正则配置不是一次性动作
///
/// regex config 不是:
///
/// ```text
/// 把当前已经存在的 logger 都改掉.
/// ```
///
/// 它也不是:
///
/// ```text
/// 当前动作 + 未来规则.
/// ```
///
/// 它是一条被保存的、有序的匹配规则.
///
/// 它只参与配置解析.
///
/// 这是因为 logger 通常是懒创建的.
///
/// 如果 regex config 只影响当前已经存在的 logger,
/// 那么未来创建的 logger 是否受影响就会变得非常反直觉.
///
/// 例如:
///
/// ```text
/// config(regex com\.abc\..*, DEBUG)
/// ```
///
/// 如果这只是一次性动作, 那么:
///
/// ```text
/// 已经存在的 com.abc.Test 可能被改成 DEBUG.
/// 未来创建的 com.abc.NewService 是否是 DEBUG？
/// ```
///
/// 这个问题没有自然答案.
///
/// 如果设计成"当前动作 + 未来规则"的混合模型, 又会出现删除语义问题:
///
/// ```text
/// 删除 regex 规则后, 已经被改过的 logger 要不要回滚？
/// 如果之后又被其他配置改过, 怎么回滚？
/// 如果多个 regex 规则叠加过, 回滚到哪一步？
/// ```
///
/// 因此, ScxLogging 不直接修改 logger 对象上的配置.
///
/// logger 的最终配置始终来自:
///
/// ```text
/// rootConfig + 当前有效规则列表
/// ```
///
/// 已有 logger 之所以能看到配置变化, 是因为配置版本变化后,
/// logger 缓存的解析结果失效, 并在下次需要时重新解析.
///
/// regex config 本身始终是一条规则, 而不是一次性命令.
///
/// ---
///
/// ## 13. removeConfig(...) 的语义
///
/// ```java
/// removeConfig(key)
/// ```
///
/// 表示:
///
/// ```text
/// 删除 key 当前有效的声明规则.
/// ```
///
/// 删除后, 该规则不再参与之后的配置解析.
///
/// removeConfig(...) 不是对 logger 对象执行回滚.
/// 它也不需要知道哪些 logger 曾经受这条规则影响.
///
/// 因为 logger 的最终配置不是存储在 logger 对象里的永久状态,
/// 而是由当前配置状态解析得出.
///
/// 删除规则后:
///
/// ```text
/// 配置版本变化.
/// 已有 logger 的缓存失效.
/// 下次读取配置时重新解析.
/// ```
///
/// 最终结果自然反映删除后的规则集合.
///
/// ---
///
/// ## 14. 内部实现可以使用 Map, 但心智模型必须是列表
///
/// ScxLogging 的心智模型是:
///
/// ```text
/// rootConfig + ordered rules list
/// ```
///
/// 也就是:
///
/// ```text
/// [rule0, rule1, rule2, ...]
/// ```
///
/// 每条 rule 都有:
///
/// ```text
/// matcher
/// config
/// declarationOrder
/// ```
///
/// 但是, 内部实现不一定真的只能使用 List.
///
/// 为了支持:
///
/// ```text
/// 1. 同一个 exact name 只保留当前有效声明.
/// 2. 同一个 regex pattern 只保留当前有效声明.
/// 3. 通过 key 快速查找和删除.
/// 4. 重新 config 同 key 时使旧声明失效.
/// ```
///
/// 实现上可以使用 Map.
///
/// 例如:
///
/// ```text
/// Map<name, rule>
/// Map<patternKey, rule>
/// ```
///
/// 也可以给每条 rule 分配递增的 declarationOrder.
///
/// 然后在解析时, 将匹配的规则按 declarationOrder 排序并应用.
///
/// 这只是实现手段.
///
/// 对用户来说, ScxLogging 不应该被理解成:
///
/// ```text
/// 两张 Map, 一张 exact, 一张 regex.
/// ```
///
/// 而应该被理解成:
///
/// ```text
/// 一条按声明时间排列的规则序列.
/// ```
///
/// Map 只是为了实现以下能力:
///
/// ```text
/// 同 key 去重.
/// 快速读取.
/// 快速删除.
/// 快速替换当前有效声明.
/// ```
///
/// 它不改变配置语义.
///
/// 即使内部有 EXACT_CONFIGS 和 REGEX_CONFIGS 两个 Map,
/// 它们也不代表 exact 和 regex 是两个优先级层.
///
/// 优先级只来自 declarationOrder.
///
/// ---
///
/// ## 15. 同 key 去重的意义
///
/// ScxLogging 不保留同一个 key 的多条历史声明.
///
/// 例如:
///
/// ```java
/// config("com.abc.Test", ERROR)
/// config("com.abc.Test", DEBUG)
/// config("com.abc.Test", WARNING)
/// ```
///
/// 逻辑上可以理解为用户进行了三次声明.
///
/// 但当前有效的只有最后一次:
///
/// ```text
/// name com.abc.Test level = WARNING
/// ```
///
/// 前两次已经被同 key 的后续声明取代.
///
/// 这不是因为 ScxLogging 是"编辑表格"模型.
///
/// 而是因为对于同一个 key, ScxLogging 只保留当前有效声明,
/// 避免无意义的历史规则继续参与解析.
///
/// 最终效果等价于:
///
/// ```text
/// 旧声明失效.
/// 新声明追加到最后.
/// ```
///
/// 这同时满足:
///
/// ```text
/// 1. config(...) 是当前声明.
/// 2. 后声明具有最新优先级.
/// 3. 同 key 不会无限累积历史规则.
/// ```
///
/// ---
///
/// ## 16. PatternKey 的意义
///
/// 对 regex 配置来说, Pattern 对象本身不适合作为用户心智上的 key.
///
/// 用户真正声明的是:
///
/// ```text
/// pattern 字符串 + flags
/// ```
///
/// 因此实现中可以使用 PatternKey, 将以下两者作为同一个 regex key:
///
/// ```java
/// regex.pattern()
/// regex.flags()
/// ```
///
/// 如果 pattern 字符串和 flags 都相同, 就认为是同一个 regex 配置 key.
///
/// 再次 config 相同 PatternKey 时, 旧 regex 声明失效,
/// 新 regex 声明成为当前声明, 并移动到声明顺序末尾.
///
/// ---
///
/// ## 17. 为什么不引入 final / stop 规则？
///
/// 有些规则系统会支持:
///
/// ```text
/// final
/// stop
/// break
/// first match wins
/// ```
///
/// 也就是命中某条规则后停止继续处理.
///
/// ScxLogging 第一版不引入这种能力.
///
/// 原因是它会增加一套额外心智模型:
///
/// ```text
/// 有些规则命中后继续合并.
/// 有些规则命中后停止合并.
/// 有些字段可能被 stop.
/// 有些字段可能不被 stop.
/// ```
///
/// 这会削弱当前模型的简单性.
///
/// 当前模型只有一条主规则:
///
/// ```text
/// 所有匹配规则都参与合并, 后面的覆盖前面的同字段声明.
/// ```
///
/// 如果未来引入 final / stop, 必须作为重大语义扩展,
/// 而不是隐藏地加入当前模型.
///
/// ---
///
/// ## 18. 与传统 logger hierarchy 的关系
///
/// 许多日志系统使用层级式配置.
///
/// 例如:
///
/// ```text
/// root
/// root.com
/// root.com.abc
/// root.com.abc.Test
/// ```
///
/// 或者:
///
/// ```text
/// root -> package/category -> logger
/// ```
///
/// 在这种模型中, logger name 的层级结构本身参与配置继承.
///
/// ScxLogging 不采用 logger hierarchy 作为核心模型.
///
/// ScxLogging 的 loggerName 只是一个字符串.
///
/// 它是否匹配某条配置, 完全由 matcher 决定.
///
/// 例如:
///
/// ```text
/// name  com.abc.Test
/// regex com\.abc\..*
/// regex .*Test
/// ```
///
/// 这些 matcher 都可以匹配同一个 loggerName.
///
/// 匹配成功之后, 不再比较它们谁更像"父级"、谁更像"子级"、
/// 谁更具体、谁更宽泛.
///
/// 只按声明顺序处理.
///
/// 这使 ScxLogging 的模型更通用,
/// 也避免了 regex 与 hierarchy 混用时的"精确度比较"问题.
///
/// ---
///
/// ## 19. 配置文件与运行期 API 的关系
///
/// 配置文件天然是一组声明.
///
/// 例如:
///
/// ```text
/// root:
///     level = ERROR
///
/// rules:
///     regex com\.abc\..*      level = DEBUG
///     name  com.abc.Test      level = WARNING
/// ```
///
/// 读取配置文件时, 应当按文件中的顺序依次声明.
///
/// 因此, 配置文件中的后续项覆盖前面的匹配项.
///
/// 如果配置文件中出现相同 key,
/// 后面的声明应视为该 key 的当前声明.
///
/// 这和运行期 API 的语义一致:
///
/// ```text
/// 后出现的声明更新.
/// ```
///
/// 配置文件不是一个特殊模式.
/// 它只是声明序列的持久化形式.
///
/// ---
///
/// ## 20. 考虑过但没有采用的方案
///
/// ### 20.1 exact 永远优先于 regex
///
/// 该方案规则类似:
///
/// ```text
/// root < regex < exact
/// ```
///
/// 优点:
///
/// ```text
/// 符合部分传统日志框架直觉.
/// 精确配置看起来更"具体".
/// ```
///
/// 缺点:
///
/// ```text
/// regex 之间仍然需要顺序.
/// exact 又被放进隐藏最高层.
/// 产生两套优先级规则.
/// 后声明 regex 无法覆盖早先 exact.
/// 不利于运行期追加调试规则.
/// ```
///
/// ScxLogging 没有采用该方案.
///
/// ### 20.2 传统 logger hierarchy
///
/// 该方案规则类似:
///
/// ```text
/// root -> package -> class
/// ```
///
/// 优点:
///
/// ```text
/// 传统.
/// 熟悉.
/// 对纯包名场景自然.
/// ```
///
/// 缺点:
///
/// ```text
/// 与 regex matcher 混用后, 精确度比较变复杂.
/// loggerName 被强行解释成层级路径.
/// 不适合所有 matcher 都平等参与的模型.
/// ```
///
/// ScxLogging 没有采用该方案.
///
/// ### 20.3 配置表编辑模型
///
/// 该方案把内部状态理解为:
///
/// ```text
/// Map<key, config>
/// ```
///
/// config(...) 表示修改表中一行.
///
/// 优点:
///
/// ```text
/// 对某些"配置管理后台"场景直观.
/// 可以修改值而不改变顺序.
/// ```
///
/// 缺点:
///
/// ```text
/// 用户必须理解 key 是否已经存在.
/// 用户必须理解修改旧行是否改变优先级.
/// config(...) 的语义依赖内部已有状态.
/// 运行期调用不像"当前声明", 而像"编辑内部文档".
/// ```
///
/// ScxLogging 核心 API 没有采用该方案.
///
/// ### 20.4 regex 作为一次性动作
///
/// 该方案将 regex config 理解为:
///
/// ```text
/// 立即修改当前所有匹配 logger.
/// ```
///
/// 优点:
///
/// ```text
/// 实现表面上简单.
/// ```
///
/// 缺点:
///
/// ```text
/// 无法自然处理未来懒创建 logger.
/// 删除 regex 时无法自然回滚.
/// 多条规则叠加后状态难以解释.
/// ```
///
/// ScxLogging 没有采用该方案.
///
/// ### 20.5 局部 update 保持旧优先级
///
/// 该方案提供:
///
/// ```java
/// updateConfig(key, updater)
/// ```
///
/// 并规定:
///
/// ```text
/// 只修改已有规则的值, 不改变其声明顺序.
/// ```
///
/// 优点:
///
/// ```text
/// 可以作为配置文档编辑工具使用.
/// ```
///
/// 缺点:
///
/// ```text
/// 引入"声明"和"编辑"两套语义.
/// 用户需要理解什么时候是新声明, 什么时候是旧规则编辑.
/// 与 config(...) 的当前声明语义冲突.
/// 增加 API 解释成本.
/// ```
///
/// ScxLogging 核心层不采用该方案.
///
/// 如果未来真的需要配置文档编辑能力,
/// 应该在更外层提供独立的配置模型或 builder,
/// 而不是污染 ScxLogging 核心配置语义.
///
/// ---
///
/// ## 21. 最终设计原则
///
/// ScxLogging 的配置系统遵循以下原则:
///
/// ```text
/// 1. rootConfig 是完整基础配置, 永远最先应用.
///
/// 2. 除 rootConfig 外, 所有配置都是有序声明规则.
///
/// 3. exact name 和 regex pattern 都只是 matcher 类型, 它们不是优先级层.
///
/// 4. matcher 只负责判断规则是否适用于 loggerName.
///
/// 5. 声明顺序负责决定匹配规则之间的覆盖关系.
///
/// 6. ScxLoggerConfig 负责声明要覆盖哪些字段.
///
/// 7. null 表示不声明该字段, 不覆盖之前结果.
///
/// 8. 空集合表示明确声明为空, 会覆盖之前结果.
///
/// 9. config(key, config) 表示当前声明.如果 key 已存在, 旧声明失效, 新声明位于最后.
///
/// 10. removeConfig(key) 删除 key 当前有效声明.
///
/// 11. regex 配置是持续规则, 不是一次性动作.
///
/// 12. logger 的最终配置由当前配置状态解析得到, 而不是被配置 API 直接永久写入 logger 对象.
///
/// 13. 内部可以使用 Map 优化查找和去重, 但用户心智模型始终是有序规则列表.
///
/// 14. ScxLogging 核心 API 不提供"就地编辑旧规则且保持旧优先级"的语义.
/// ```
///
/// ---
///
/// ## 总结
///
/// ScxLogging 选择的是:
///
/// ```text
/// rootConfig + ordered declaration rules
/// ```
///
/// 而不是:
///
/// ```text
/// root < regex < exact
/// ```
///
/// 也不是:
///
/// ```text
/// root < package < logger
/// ```
///
/// 更不是:
///
/// ```text
/// 内部配置表编辑器
/// ```
///
/// 这套模型的核心是:
///
/// ```text
/// 用户发出配置声明.
/// 声明具有顺序.
/// 后声明的匹配规则覆盖先声明的匹配规则.
/// ```
///
/// exact 和 regex 的区别只在 matcher 行为, 不在优先级层级.
///
/// config(...) 的含义是:
///
/// ```text
/// 让这条配置现在生效.
/// ```
///
/// 因此用户不需要知道内部之前是否已经存在同 key 的配置.
/// 无论存在与否, 这次 config 都是当前声明, 具有最新优先级.
///
/// 这让 ScxLogging 的配置语义保持统一:
///
/// ```text
/// 匹配由 matcher 决定.
/// 覆盖由声明顺序决定.
/// 字段合并由 ScxLoggerConfig 决定.
/// ```
///
/// 只要这三件事保持分离, 整个配置模型就是确定、可解释、可扩展的.
///
/// # 关于并发语义.
///
/// ScxLogging 的配置 API 支持并发调用, 但不提供多线程配置写入的强线性化顺序保证.
///
/// 当多个线程同时修改日志配置, 尤其是同时修改或删除同一个 name / regex 配置时, 最终生效结果以并发执行结果为准, 不保证与调用发起时间严格一致.
///
/// 配置变更会递增全局配置版本.ScxLogger 会在后续日志调用中检测版本变化并重新解析配置.
///
/// 正在执行中的 log 调用可能继续使用旧配置, 也可能在并发修改期间解析到弱一致状态.ScxLogging 只保证配置最终传播到 logger 缓存, 不保证每一次日志调用都观察到最新配置.
///
/// @author scx567888
public final class ScxLogging {

    // 存储所有日志
    private static final ConcurrentHashMap<String, ScxLogger> LOGGERS = new ConcurrentHashMap<>();

    // 正则配置
    private static final ConcurrentHashMap<PatternKey, ConfigEntity> REGEX_CONFIGS = new ConcurrentHashMap<>();

    // 精确配置
    private static final ConcurrentHashMap<String, ConfigEntity> EXACT_CONFIGS = new ConcurrentHashMap<>();

    // CONFIG 版本
    private static final AtomicLong CONFIG_VERSION = new AtomicLong(0);

    // CONFIG 索引
    private static final AtomicLong CONFIG_INDEX = new AtomicLong(0);

    // 根配置
    private static volatile ScxLoggerConfig ROOT_CONFIG = new ScxLoggerConfig(ERROR, false, List.of(new ConsoleRecorder()));

    // ****************** logger 相关 ******************

    public static ScxLogger getLogger(String name) {
        if (name == null) {
            throw new NullPointerException("name can not be null");
        }
        return LOGGERS.computeIfAbsent(name, ScxLogger::new);
    }

    public static ScxLogger getLogger(Class<?> clazz) {
        if (clazz == null) {
            throw new NullPointerException("clazz can not be null");
        }
        return getLogger(clazz.getName());
    }

    // ****************** 根配置相关 ******************

    public static ScxLoggerConfig rootConfig() {
        return ROOT_CONFIG;
    }

    public static void rootConfig(ScxLoggerConfig rootConfig) {
        if (rootConfig == null) {
            throw new NullPointerException("rootConfig can not be null");
        }
        // root 配置 所有字段都要求有值
        if (rootConfig.level() == null) {
            throw new IllegalArgumentException("rootConfig level can not be null");
        }
        if (rootConfig.stackTrace() == null) {
            throw new IllegalArgumentException("rootConfig stackTrace can not be null");
        }
        if (rootConfig.recorders() == null) {
            throw new IllegalArgumentException("rootConfig recorders can not be null");
        }
        ROOT_CONFIG = rootConfig;
        configChanged();
    }

    // ****************** 正则配置相关 ******************

    public static ScxLoggerConfig config(Pattern regex) {
        if (regex == null) {
            throw new NullPointerException("regex can not be null");
        }
        var entity = REGEX_CONFIGS.get(new PatternKey(regex));
        if (entity == null) {
            return null;
        }
        return entity.config();
    }

    public static void config(Pattern regex, ScxLoggerConfig newConfig) {
        if (regex == null) {
            throw new NullPointerException("regex can not be null");
        }
        if (newConfig == null) {
            throw new NullPointerException("newConfig can not be null");
        }
        REGEX_CONFIGS.put(new PatternKey(regex), new ConfigEntity(nextConfigIndex(), newConfig));
        configChanged();
    }

    public static void removeConfig(Pattern regex) {
        if (regex == null) {
            throw new NullPointerException("regex can not be null");
        }
        var remove = REGEX_CONFIGS.remove(new PatternKey(regex));
        if (remove != null) {
            configChanged();
        }
    }

    // ****************** 精确配置相关 ******************

    public static ScxLoggerConfig config(String name) {
        if (name == null) {
            throw new NullPointerException("name can not be null");
        }
        var entity = EXACT_CONFIGS.get(name);
        if (entity == null) {
            return null;
        }
        return entity.config();
    }

    public static void config(String name, ScxLoggerConfig newConfig) {
        if (name == null) {
            throw new NullPointerException("name can not be null");
        }
        if (newConfig == null) {
            throw new NullPointerException("newConfig can not be null");
        }
        EXACT_CONFIGS.put(name, new ConfigEntity(nextConfigIndex(), newConfig));
        configChanged();
    }

    public static void removeConfig(String name) {
        if (name == null) {
            throw new NullPointerException("name can not be null");
        }
        var remove = EXACT_CONFIGS.remove(name);
        if (remove != null) {
            configChanged();
        }
    }

    // ****************** 内部方法 ******************

    static long configVersion() {
        return CONFIG_VERSION.get();
    }

    static void configChanged() {
        CONFIG_VERSION.incrementAndGet();
    }

    static long nextConfigIndex() {
        return CONFIG_INDEX.incrementAndGet();
    }

    /// 解析当前 loggerName 的配置.
    ///
    /// 返回的 ScxLoggerConfig 所有字段都不允许 为 null
    ///
    /// 关于并发语义.
    ///
    /// 注意:这里不追求严格快照一致性.
    ///
    /// REGEX_CONFIGS / EXACT_CONFIGS 使用 ConcurrentHashMap, 允许并发修改期间解析.
    ///
    /// 本次解析可能看到配置变更前后的混合状态；
    ///
    /// 但配置变更会更新 CONFIG_VERSION, 后续 logger 会重新解析配置.
    static ScxLoggerConfig resolveConfig(String name) {
        // 1, 根配置
        var rootConfig = ROOT_CONFIG;

        var level = rootConfig.level();
        var stackTrace = rootConfig.stackTrace();
        var recorders = rootConfig.recorders();

        var configEntities = new ArrayList<ConfigEntity>();

        // 2, 正则配置
        for (var e : REGEX_CONFIGS.entrySet()) {
            var pattern = e.getKey().pattern();
            var regexConfig = e.getValue();
            var matcher = pattern.matcher(name);
            if (matcher.matches()) {
                configEntities.add(regexConfig);
            }
        }

        // 3, 精确配置
        var exactConfig = EXACT_CONFIGS.get(name);

        if (exactConfig != null) {
            configEntities.add(exactConfig);
        }

        // 排序
        configEntities.sort(Comparator.comparingLong(ConfigEntity::index));

        for (var configEntity : configEntities) {
            var config = configEntity.config;
            if (config.level() != null) {
                level = config.level();
            }
            if (config.stackTrace() != null) {
                stackTrace = config.stackTrace();
            }
            if (config.recorders() != null) {
                recorders = config.recorders();
            }
        }

        return new ScxLoggerConfig(level, stackTrace, recorders);
    }

    /// 包含 index 的简单封装.
    private record ConfigEntity(long index, ScxLoggerConfig config) {

    }

    /// 只使用 pattern(字符串) + flags 作为比较对象 .
    private static final class PatternKey {

        private final Pattern pattern;

        public PatternKey(Pattern pattern) {
            this.pattern = pattern;
        }

        public Pattern pattern() {
            return pattern;
        }

        @Override
        public boolean equals(Object object) {
            if (object instanceof PatternKey patternKey) {
                return pattern.pattern().equals(patternKey.pattern.pattern()) && pattern.flags() == patternKey.pattern.flags();
            }
            return false;
        }

        @Override
        public int hashCode() {
            var regex = pattern.pattern();
            var flags = pattern.flags();
            int result = regex.hashCode();
            result = 31 * result + flags;
            return result;
        }

    }

}
