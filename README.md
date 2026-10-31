# ikun

Minecraft 26.1.2 Fabric，Java 25。提供 KuudraParty、Auto Cannon Close、AutoGFS、Kuudra Shop、Supply Swap、Pearl Supply Interact、Rend Rotate 与配置 GUI。

将 JAR 放入 `mods`，替换旧的 IloveKu JAR，并移除之前的 NoammAddons/KuudraParty JAR。需要 Fabric API 和 Fabric Language Kotlin。
使用 `/ikun`、右 Shift 或 Mod Menu 打开设置。左键功能名称切换开关，右键打开选项。
ClickGUI 右上角 `Colors` 打开全局配色：可调整 Theme Color（强调色）、Panel Color（面板及控件底色）、Background Color（屏幕背景）、Text Color、Muted Text Color，以及面板和背景透明度。调色实时应用到分类、功能按钮、设置窗口、滑块、搜索、文本输入与提示框；各颜色和透明度控件右键恢复默认，`Reset Theme` 一次恢复整套默认配色。关闭 GUI 时保存到 `config/ikun/config.json` 的 `clickGuiTheme`，旧配置自动使用默认蓝黑主题。Kuudra Shop 单独设置的按钮底色和透明度继续独立保存。
配置保存于 `config/ikun/config.json`。首次使用新名称时，如果没有新配置，会导入 `config/IloveKu/config.json`，保留各功能开关、商店按键、位置与大小；原配置文件保留。
按 Esc 或当前绑定的 GUI 快捷键（默认右 Shift）直接关闭整个界面；录入商店按键时优先处理该次输入，Esc 取消录入，松开后再次按 Esc 才关闭。保存失败会记录到日志，界面仍正常关闭。
新安装默认开启入队统计；AutoKick 及各项踢人检查默认关闭。首次运行尝试导入原 NA 默认配置中的 KuudraParty 设置，其余设置不导入。

KuudraParty 依据 `IloveKu-1.0.4-26.1.2.jar` 的原有逻辑移植：队员加入后查询并展示所选难度的完成次数、Golden Dragon、RCM、TAP、Hyp、Term 和 Open Shop，悬停玩家信息可查看返回的装备。保留 T5、RCM Terror、TAP、No Hyp 和 API 复查的可选自动踢人；AutoKick 要求当前玩家是队长，查询失败不会触发自动踢人。
聊天结果附带手动重新查询与踢人入口，查询失败时也能点击重查。重查会请求刷新数据，同玩家查询进行中不会重复启动；手动踢人不依赖 AutoKick 开关，但执行时必须仍是队长，且本人、目标仍在当前队伍，不能踢自己，同一按钮不会重复发送踢人命令。动作绑定世界和队伍会话，关闭功能、切换世界或队伍会话变化后，旧按钮会失效；同玩家重新入队会替换旧动作。
聊天按钮使用本地命令 `/ikun retry <内部id>` 和 `/ikun kick <内部id>`，内部 id 由本次入队记录生成，不是玩家名，不需要手工填写；不会把这些管理命令发送到服务器。通过校验的踢人操作才会发送服务器的队伍踢人命令。
HTTPS 查询地址内置，不显示在 GUI、聊天报错或新配置文件中。网络分析或反编译仍可看到地址；Hypixel key 仅在后端。
当前是临时 Cloudflare 隧道，地址变化后需更新 `QUERY_ENDPOINT` 并重新打包。

源码独立于原 NA，没有原版其他功能、自动更新、账户认证、WebSocket、远程功能控制或恶作剧。使用商店绘制、Supply Swap 与 Pearl Supply Interact 物品交互 Mixin，保留原服务器容器。
GUI 控件来自 NoammAddons，队伍聊天解析保留原文件署名。原 CC0 许可保留在 `licenses/ikun-original-CC0.txt`；Rend Rotate 转头代码改编自 RatTokens 的 RatAddons 2.0.1，组合发行包采用 GPL-3.0-only，来源与许可见 `NOTICE.md` 和 `LICENSE`。

构建：`./gradlew test build --console=plain`，需要 Java 25。

精简构建使用 Java 自带 HTTP 客户端及 Minecraft 原生 GUI/聊天/音效接口，不打包 Ktor、UniversalCraft 或重复的 Kotlin 运行库。
Fabric API 和 Fabric Language Kotlin 仍需单独安装；Mod Menu 可选。JAR 体积限制为严格小于 1,000,000 字节，`check`/`build` 会自动检查。
发布检查：`python tools/verify_release.py build/libs/ikun-1.0.0-26.1.2.jar`。

Rend Rotate 默认关闭，在 ClickGUI 的 Cheat 分类左键开启，右键调整 Rotation Delay、Yaw/Pitch Offset Min/Max 和 Front/Right/Left/Back Min/Max。只提取 RatAddons 2.0.1 的 Rend 转头功能；不包含自动移动、潜行、跳跃、切物品、Hollow、甩杆、丢骨、换装或拉 Rend 操作。
原版触发条件保留：Kuudra Stun/DPS 阶段、未被标记为 FINAL KILLED，收到服务器位置包落在 X[-102,-101]、Y[5,7]、Z[-106,-105] 时，按原客户端 tick 调度延迟转头。通过大小 30、基础最大生命值 100000 的存活 MagmaCube 识别 Kuudra；方向优先级、四个固定目标点和基础 Pitch -30° 均保留。
转头逐帧运行，保留原平滑曲线、随机速度、过冲修正、微调及鼠标灵敏度步长；速度默认 Front 85–145、Right/Left 235–295、Back 285–345，偏移默认 0，Rotation Delay 默认 5。Front/Right/Left/Back 参数是转向速度，不是目标角度。配置保存为 `rendRotate`、`rendRotateSettings`；旧配置没有这些字段时保持关闭。关闭功能、切世界或断开连接会清除排队和正在进行的转头。

Auto Cannon Close 默认关闭，可在 ClickGUI 的 GENERAL 面板独立开启。收到 `You purchased Human Cannonball!` 后，在接下来的 500 毫秒内关闭标题包含 `Perk Menu` 的容器界面。关闭功能或切换世界会取消待关闭状态。开关保存于原配置文件的 `autoCannonClose` 字段，兼容原 KuudraParty 配置。
该功能依据 RatAddons 1.1.0 的触发规则独立实现；发布包不包含 RatAddons 类。

AutoGFS 默认关闭，在 ClickGUI 的 GENERAL 面板左键开启，右键配置 `Refill Pearl`（默认开启）和 `Toxic Amount`（默认 16，范围 1–32）。珍珠补充依据 `IloveKu-1.0.4-26.1.2.jar` 的原有规则移植，Toxic 触发按下文改用 RatAddons 2.0.1 规则；主开关和子设置分别保存于 `config/ikun/config.json` 的 `autoGfs`、`autoGfsSettings` 字段；旧配置缺少主开关时保持关闭。
识别进入 Kuudra 后等待 10 秒首次检查珍珠，此后每 20 秒检查一次；统计背包、快捷栏和副手中的末影珍珠，不足 16 时发送 `/getfromsacks ENDER_PEARL <差额>`。关闭 `Refill Pearl` 后停止珍珠补充；打开 GUI 或玩家死亡时暂停补充。
Toxic 触发改用 RatAddons 2.0.1 `KittyKuudraBasics.handleChat` 的规则：在 Kuudra 内，去除格式码的聊天文本包含大小写一致的 `The Ballista is finally ready!` 时，发送 `/gfs toxic_arrow_poison <Toxic Amount>`。这是弩炮建造完成的台词，比原先 `shoot him with the Ballista!` 的射击提示更早；旧射击提示不再触发。沿用原版子串匹配，不额外要求 NPC 前缀，也不限制同一世界一次，因此包含该短句的玩家聊天和同局重复提示也会触发。数量保留 ikun 原设置（默认 16，范围 1–32），不检查现有毒药数量，不新增 Twilight。珍珠补充逻辑不变，切换世界重置珍珠检查计时。该功能只使用现有事件与客户端 tick，不新增 Mixin；补给命令是否成功由服务器决定。

Supply Swap 默认关闭，在 ClickGUI 的 GENERAL 面板开启。Kuudra 场景内主手拿着 `Elle's Supplies` 右键，会在随后一次客户端 tick 结束时切到快捷栏中的末影珍珠并使用，按当前视角投掷，不自动切回。行为依据 RatAddons 1.2.2：快捷栏没有珍珠时仍拦截物资的原右键操作；关闭功能或切换世界会清除待投掷状态。

Pearl Supply Interact 默认关闭，在 ClickGUI 的 Cheat 分类开启。主手持末影珍珠对着地面或其他方块右键时，跳过方块交互，让原版继续处理珍珠投掷；方向跟随当前视角。行为沿用 IQ 的原始逻辑，主手持珍珠时全局生效，副手沿用原版行为。开关保存于 `config/ikun/config.json` 的 `pearlSupplyInteract` 字段，旧配置缺少该字段时默认关闭。

Kuudra Shop 默认关闭，在 ClickGUI 的 GENERAL 面板左键开启。Hypixel 的 `Perk Menu` 会显示与 ClickGUI 一致的直角按钮，包含原物品图标、名称、价格/状态摘要及悬停原物品说明。界面只绘制保留的按钮，不显示 Kuudra Shop 标题、Original menu 或 X 关闭按钮，也不提供翻页。左键或各功能绑定的按键点击对应按钮。Esc 关闭；背包键若已绑定某个商店功能，则优先执行该绑定，否则用于关闭。
ClickGUI 右键 `Kuudra Shop` → `Appearance`，通过 `Background Color` 的色相条和颜色区域选取背景色，通过 `Background Opacity` 设置不透明度（0% 全透明，100% 不透明）。右键调色区或滑条恢复默认值。职业、工作台、大炮、Confirm/Cancel 以及 HUD Edit 预览统一使用这套颜色和透明度，悬停时使用 ClickGUI 的高亮样式，文字和物品图标保持清晰。颜色及透明度保存于 `kuudraShopSettings`，旧配置缺少这两项时使用 ClickGUI 的默认深色。
默认按键：职业入口 1，工作台 1，人体大炮 2，Fill your Quiver 3，Confirm 1，Cancel 2。每张卡片按功能保留自己的按键；购买后暂时消失、升级或顺序改变，不会使其他按键重新分配；目标缺失时对应按键不执行操作。HUD Edit 与实际商店都显示当前绑定键名，解绑时隐藏键名。
ClickGUI 右键 `Kuudra Shop` → `Keybinds` 可分别设置 `Class Key`、`Ballista Key`、`Cannon Key`、`Fill your Quiver Key`、`Confirm Key` 和 `Cancel Key`。左键绑定项后直接按想用的数字、字母或功能键；Backspace/Delete 解绑，Esc 取消录入，右键恢复默认。若默认键已被同页另一功能占用，会保留当前值并提示换键。同页职业/工作台/大炮与 Fill your Quiver 之间、Confirm/Cancel 之间不允许重复绑定；不同页可以共用键。普通键盘按键可单独绑定，Esc 保留为关闭/取消；按键保存在原配置的 `kuudraShopSettings` 字段，旧配置缺少绑定时使用默认值。按住键只触发一次；服务器切换商店或确认页时共用按键状态，必须松开再按，防止职业选择键长按透传到 Confirm。
商店及确认页已移除大背景板和全屏暗色遮罩，保留各卡片本身的底色。ClickGUI 右键 `Kuudra Shop` → `HUD Edit`，或使用 `/ikun hudedit` 打开编辑器。在 Class / Perks / Confirm 三页分别调整职业、工作台、大炮、Fill your Quiver 和确认/取消卡片。左键拖动位置；鼠标悬停某张卡片后滚轮仅调整该卡片大小，0.1–2.0 倍，步长 0.1，默认 1.0。滚动空白处或工具栏不会改变其他卡片。右键卡片恢复它的默认位置和大小，`Reset` 或 R 重置当前页。`Done` 或 Esc 保存并返回，编辑预览不会购买物品。
每张卡片按功能独立保存位置和大小，等级、价格或服务器槽位变化不会改变它们；缩放和分辨率变化时保留卡片中心的相对屏幕位置，并限制卡片移出屏幕。位置和大小保存于原配置的 `kuudraShopPositions` 字段，旧配置的位置继续保留，缺少独立大小时使用 1.0 倍。实际商店与编辑预览采用相同的等比缩放，背景、图标、文字及鼠标命中范围同步调整。卡片重叠时，点击和编辑与最上层卡片对应。
在 ClickGUI 右键 `Kuudra Shop`，可通过 `Render Scale` 滑条调整商店及确认页大小：0.1–2.0 倍，步长 0.1，默认 1.0。左键拖动滑条、悬停滚轮微调，右键设置重置为 1.0。小窗口会自动限制实际放大幅度以保证按钮可见，鼠标点击位置随缩放同步调整。关闭 ClickGUI 后保存，旧配置缺少该设置时保持默认大小。
职业选择页显示第二排的钻石剑入口和末格的 Fill your Quiver / Open Shop；钻石剑默认位于屏幕正中央，需手动点击；本页其他职业、复活、大炮、Remote Perk Shop、钓竿、镐和其他物品均隐藏。识别第二排同时包含钻石剑及金胸甲的职业选择布局。选择后进入的商店显示 Ballista Mechanic（工作台）、Human Cannonball（人体大炮）和末格的 Fill your Quiver / Open Shop，Auto Revive 已移除。按物品名称识别，支持 Ballista Mechanic 等级变化，点击映射仍使用实际服务器槽位；工作台卡片显示下一等级效果。HUD Edit 中已有的自定义位置会保留，旧复活位置被忽略，重置对应页即可恢复居中默认布局。
商品来自服务器当前槽位，排除玩家背包及装饰玻璃，不自动购买。购买前检查原容器和物品是否仍与画面一致，价格及是否购买成功由服务器决定。兼容 Auto Cannon Close，开关保存于 `kuudraShop` 字段，不改变旧配置。逻辑及构建检查不能替代 Hypixel 实机验证。
从商店卡片选择职业后出现的 `Are you sure?` 确认菜单只显示居中的 Confirm / Cancel 两个小按钮及各自绑定键名，不显示标题、提示段落、物品说明或其他工具按钮。通过物品名称定位原槽位，手动左键或绑定键确认/取消；默认数字键 1 确认、2 取消。选择职业后必须先松开原按键，再按 Confirm 的绑定键，Esc 仍可关闭。只有商店操作后 10 秒内出现且带有可识别确认/取消按钮的菜单会被接管；其他确认菜单保持原界面。

Fill your Quiver 按钮映射到服务器容器最后一排最后一格（6 行菜单的槽位 53），同时支持购买前的 `Fill your Quiver` 和购买后的 `Open Shop` 名称。默认按键 3，右键 Kuudra Shop 可通过 `Fill your Quiver Key` 改键，HUD Edit 可独立调整位置和大小；职业页与 Perks 页共用其布局。旧配置若已将 3 分配给同页其他按钮，新按钮默认解绑以保留已有绑定。点击使用服务器的原物品与原槽位，不自动购买或补箭；随后打开的原版交易界面保持原样。

末格按钮在商店、悬停说明和 HUD Edit 中统一显示为 `Fill your Quiver`，按键设置名为 `Fill your Quiver Key`；继续识别服务器的两种名称，并兼容旧的 `Open Shop Key` 配置。
