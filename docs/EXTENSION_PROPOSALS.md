# kanso 拡張案の詳細検討 (Extension Proposals Review)

kanso デザインシステムの現状を棚卸しし、消費アプリ (LMSA / CertWatch / Semicon News /
medcal) の実需要から逆算した推奨拡張案を、優先度・API スケッチ・設計上の論点・工数・
リスクまで含めて検討する。

判断基準は kanso の哲学そのもの:

> **簡素 — nothing decorative, everything deliberate.**
> トークンから読む。ハードコードしない。アプリが 2 回以上書くものだけをシステムに昇格する。

---

## 1. 現状の棚卸し

### theme/

| ファイル | 提供物 | 状態 |
|---|---|---|
| `KansoTheme.kt` | テーマ入口・`Kanso` アクセサ・`KansoBrands` | dynamic color / edge-to-edge 対応済み |
| `Color.kt` | seed → M3 ColorScheme(HSL 近似トーン)| HCT 非厳密(意図的・依存ゼロ)|
| `Tokens.kt` | spacing(4dp グリッド)・elevation | 充実 |
| `Type.kt` | `Typography()`(プラットフォーム既定)| 最小 |
| `Shape.kt` | M3 コーナースケール | 充実 |

### component/

| コンポーネント | カバーする UI | 主な未カバー |
|---|---|---|
| `KansoButton`(5 段階 + icon + loading)| アクション | サイズ違い・トレーリングアイコン |
| `KansoCard` / `KansoSectionHeader` | グルーピング | クリック可能カード |
| `KansoTextField` | フォーム入力 | パスワード・選択式・IME アクション |
| `KansoScaffold` | 画面骨格 | large app bar・戻るナビの定型 |
| `KansoListItem` / `KansoStatusRow` | リスト・キー/値 | バッジ・3 行・区切り線 |
| `KansoEmptyState` / `KansoLoadingState` | 全面プレースホルダ | **エラー状態・スケルトン** |

### 構造的な空白(コンポーネント単体でなくシステムとして欠けているもの)

1. **セマンティックカラーがない** — M3 の標準ロールは `error` のみ。success / warning /
   info を表す場所がなく、アプリ側でハードコードするしかない(= トークン哲学に反する)。
2. **確認・通知系がない** — ダイアログ、ボトムシート、インラインバナー。
3. **設定画面の定型がない** — スイッチ/チェックボックス/ラジオ付きの行。
4. **`material3-window-size-class` を `api` で公開しているのに、使うコンポーネントがない。**
5. **品質基盤がない** — プレビュー規約、スクリーンショットテスト、(クラッシュ回避で
   無効化した)lint の代替。

---

## 2. 消費アプリからの逆算

| アプリ | ドメイン | kanso に欲しくなるもの(推定) |
|---|---|---|
| LMSA | セキュア通信 | 接続状態バッジ、PIN/パスワード欄、確認ダイアログ、エラー+リトライ |
| CertWatch | 証明書監視 | **有効/期限間近/失効の 3 状態表示**(success/warning/error)、状態チップ、pull-to-refresh |
| Semicon News | ニュース | pull-to-refresh、スケルトン、タグ/チップ、記事リスト密度 |
| medcal | 医療系計算 | 選択式フィールド、単位付き入力、結果の強調表示、警告バナー |

4 アプリ中 3 つが「状態を色で語る」アプリであり、**セマンティックカラーの不在が最大の
ボトルネック**。これが最優先(E1)である根拠。

---

## 3. 推奨拡張案

優先度: **P1** = 全アプリが今すぐ使う / **P2** = 体験の底上げ / **P3** = 基盤・品質。
工数: S = 半日以内, M = 1–2 日, L = 3 日以上(デモ更新込み)。

### P1 — 全アプリが今すぐ使うもの

#### E1. セマンティックカラー拡張(success / warning / info)— **最優先** `M`

**動機**: CertWatch の期限 3 状態、LMSA の接続状態、medcal の警告。M3 標準には `error`
しかなく、現状はアプリごとに緑や橙をハードコードするしかない。

**API スケッチ**(既存トークンと同じ CompositionLocal パターン):

```kotlin
@Immutable
data class KansoSemanticColors(
    val success: Color, val onSuccess: Color,
    val successContainer: Color, val onSuccessContainer: Color,
    val warning: Color, val onWarning: Color,
    val warningContainer: Color, val onWarningContainer: Color,
    val info: Color, val onInfo: Color,
    val infoContainer: Color, val onInfoContainer: Color,
)

val LocalKansoSemanticColors = staticCompositionLocalOf<KansoSemanticColors> { … }

// Kanso アクセサに追加
object Kanso {
    val semantic: KansoSemanticColors
        @Composable @ReadOnlyComposable get() = LocalKansoSemanticColors.current
}
```

**設計上の論点**:

- **導出方法**: `error` と同様に**固定 hue + 既存の tone 関数**で生成するのが一貫的
  (success ≈ hue 145°, warning ≈ hue 45°, info ≈ hue 240°。彩度は `S_PRIMARY` 系を流用)。
  seed から導出すると「緑ブランドの success」が識別不能になるので固定 hue が正しい。
- **dynamic color 時**: M3 の error と同じ扱い — wallpaper 由来スキームでも固定のまま。
  ハーモナイズ(seed の hue に数度寄せる)は将来の任意改善とし、初版ではやらない。
- ライト/ダークで tone マッピングを反転(error の実装と同じ 40/100/90/10 ↔ 80/20/30/90)。

**リスク**: 低。既存 API に加算のみ。`KansoTheme` の `CompositionLocalProvider` に
1 provider 追加。

#### E2. `KansoStatusBadge` / `KansoTag` `S`(E1 依存)

**動機**: E1 の色を「正しい形」で消費する場所。CertWatch のリスト行トレーリング、
Semicon News のカテゴリタグ。

```kotlin
enum class KansoStatus { Success, Warning, Error, Info, Neutral }

@Composable
fun KansoStatusBadge(
    text: String,
    status: KansoStatus,
    modifier: Modifier = Modifier,
    icon: ImageVector? = null,   // 例: CheckCircle / Warning
)
```

container/onContainer ペアで塗る小型ピル。`KansoListItem` の `trailing` スロットに
そのまま収まるサイズ(高さ ~24dp、`labelSmall`)。

#### E3. `KansoAlertDialog`(+ 破壊的操作バリアント) `S`

**動機**: 削除確認・ログアウト確認は全アプリ共通。素の `AlertDialog` を都度組むと
ボタンの強調順・破壊的操作の色がアプリごとにぶれる。

```kotlin
@Composable
fun KansoAlertDialog(
    title: String,
    text: String,
    confirmText: String,
    onConfirm: () -> Unit,
    onDismiss: () -> Unit,
    modifier: Modifier = Modifier,
    dismissText: String? = null,     // null なら確認のみ
    destructive: Boolean = false,    // confirm を error 色に
    icon: ImageVector? = null,
)
```

**論点**: confirm は `TextButton`(M3 標準)。`destructive = true` のとき
`colors.error` を confirm に適用。それ以上の自由度(カスタム content)は素の
`AlertDialog` に逃がし、kanso は 9 割の定型だけ持つ。

#### E4. 設定行: `KansoSwitchRow` / `KansoCheckboxRow` / `KansoRadioRow` `M`

**動機**: 設定画面は全アプリにある。`KansoListItem` + `trailing = { Switch(…) }` で
組めるが、**行全体のクリックとコントロールの状態が連動しない/セマンティクスが壊れる**
のが定番事故。`Modifier.toggleable(role = Role.Switch)` を正しく貼った定型を提供する
価値が大きい。

```kotlin
@Composable
fun KansoSwitchRow(
    headline: String,
    checked: Boolean,
    onCheckedChange: (Boolean) -> Unit,
    modifier: Modifier = Modifier,
    supporting: String? = null,
    icon: ImageVector? = null,
    enabled: Boolean = true,
)
```

**論点**: 内部は `KansoListItem` を再利用せず Row を直書きする(toggleable を行全体に
貼る必要があり、`onClick` ベースの ListItem とは責務が違う)。48dp 最小タッチターゲット
を保証。Checkbox/Radio 版は同型。

#### E5. `KansoTextField` の拡張 + `KansoPasswordField` `M`

**動機**: LMSA の PIN 欄はデモですら `KeyboardType.NumberPassword` を渡しているのに
**マスク表示(visualTransformation)ができない**。IME アクション・先頭/末尾アイコン・
readOnly も現状不可。

方針: 既存シグネチャに**後方互換の引数追加**(すべてデフォルト付き):

```kotlin
fun KansoTextField(
    …既存…,
    enabled: Boolean = true,
    readOnly: Boolean = false,
    leadingIcon: ImageVector? = null,
    trailing: @Composable (() -> Unit)? = null,
    visualTransformation: VisualTransformation = VisualTransformation.None,
    imeAction: ImeAction = ImeAction.Default,
    keyboardActions: KeyboardActions = KeyboardActions.Default,
)
```

加えてパスワード用は表示切替トグルまで内蔵した専用品を出す:

```kotlin
@Composable
fun KansoPasswordField(
    value: String, onValueChange: (String) -> Unit, label: String,
    modifier: Modifier = Modifier,
    supporting: String? = null, isError: Boolean = false, errorText: String? = null,
)  // 目アイコンで PasswordVisualTransformation ⇄ None を内部トグル
```

#### E6. `KansoErrorState` `S`

**動機**: `KansoEmptyState` は「空」と「失敗」を兼ねているが、失敗にはリトライという
固有の意味論がある。薄いラッパで意図を分離する:

```kotlin
@Composable
fun KansoErrorState(
    title: String,
    modifier: Modifier = Modifier,
    description: String? = null,
    retryText: String = "再試行",      // 実装ではリソース化を検討
    onRetry: (() -> Unit)? = null,
)  // 内部は KansoEmptyState(icon = ErrorOutline, tint は E1 の error/onSurfaceVariant)
```

実装は `KansoEmptyState` への委譲でよい(コード追加は最小、意図の固定が目的)。

### P2 — 体験の底上げ

#### E7. `KansoSelectField`(選択式フィールド) `M`

medcal の単位選択・Semicon News のフィルタ。`ExposedDropdownMenuBox` +
`OutlinedTextField(readOnly = true)` の定型は組むのが面倒で、kanso 化の価値が高い。

```kotlin
@Composable
fun <T> KansoSelectField(
    value: T?,
    options: List<T>,
    onSelect: (T) -> Unit,
    label: String,
    modifier: Modifier = Modifier,
    optionLabel: (T) -> String = { it.toString() },
    supporting: String? = null,
)
```

#### E8. Pull-to-refresh ラッパ `S`

CertWatch / Semicon News の一覧。M3 1.3+ の `PullToRefreshBox` を kanso の
インジケータ色で包むだけの薄いもの:

```kotlin
@Composable
fun KansoRefreshBox(
    refreshing: Boolean,
    onRefresh: () -> Unit,
    modifier: Modifier = Modifier,
    content: @Composable BoxScope.() -> Unit,
)
```

#### E9. `KansoSkeleton`(シマー) `M`

一覧初回ロードで `KansoLoadingState`(全面スピナー)よりも体感が良い。
`surfaceContainerHighest` ⇄ `surfaceContainerHigh` のトーン間アニメーション
(`rememberInfiniteTransition`)で依存ゼロのまま実装可能。行スケルトン
(`KansoListItemSkeleton`)を 1 種だけ出し、汎用 `Modifier.kansoSkeleton()` は
乱用を招くので出さない。

#### E10. `KansoBottomSheet` `S`

`ModalBottomSheet` の薄いラッパ(shape / dragHandle / containerColor をトークン固定)。
自由 content のまま。

#### E11. 適応ナビゲーション: `KansoNavScaffold` `L`

**動機**: `material3-window-size-class` を公開依存にしているのに未使用。デモアプリは
素の `NavigationBar` を直書きしており、各アプリも同じことをする未来が見えている。

```kotlin
data class KansoNavItem(val label: String, val icon: ImageVector, val route: String)

@Composable
fun KansoNavScaffold(
    items: List<KansoNavItem>,
    selectedRoute: String,
    onSelect: (String) -> Unit,
    title: String,
    modifier: Modifier = Modifier,
    actions: @Composable RowScope.() -> Unit = {},
    content: @Composable (PaddingValues) -> Unit,
)
// compact 幅 → NavigationBar(下)、medium 以上 → NavigationRail(横)
```

**論点**: これは「コンポーネント」より「アプリ骨格」に近く、kanso の守備範囲を広げる
判断になる。ただしタブレット/折りたたみ対応を全アプリで一貫させる唯一の現実解であり、
P2 の中では投資対効果が最も大きい。navigation-compose への結合は避け、`route: String`
のコールバックに留める(依存方向を保つ)。

#### E12. `KansoDivider` / `KansoInfoBanner` `S`

- `KansoDivider`: `outlineVariant` 色 + インセット規約(リスト内はアイコン幅分)。
- `KansoInfoBanner`: インラインの告知/警告帯(E1 の container 色 + アイコン + 任意の
  アクション)。medcal の注意書き、LMSA の劣化モード告知。スナックバー(一時)と違い
  常在する情報向け。

#### E13. `KansoScaffold` の拡張 `S`

- `largeTopBar: Boolean = false`(設定トップなど階層の起点画面用に
  `LargeTopAppBar` + `exitUntilCollapsedScrollBehavior`)。
- 戻るナビの定型: `KansoBackButton(onBack)`(`Icons.AutoMirrored.ArrowBack` +
  contentDescription 込み)を用意し、`navigationIcon` に渡すだけにする。

### P3 — 基盤・品質(コンポーネントより先に腐るのを防ぐ)

#### E14. モーショントークン `S`

```kotlin
@Immutable
data class KansoMotion(
    val quick: Int = 100,      // ms — 押下フィードバック
    val standard: Int = 250,   // 画面内トランジション
    val emphasized: Int = 400, // 画面間・大きな状態変化
    val easing: Easing = FastOutSlowInEasing,
    val emphasizedEasing: Easing = CubicBezierEasing(0.2f, 0f, 0f, 1f),
)
```

E9(シマー)や今後のアニメーションが duration をハードコードし始める前に置く。

#### E15. カラーパイプラインの HCT 正確化 — **検討の結果、当面見送りを推奨**

現状の HSL 近似には既知の限界がある:

- `hueOf` は seed の**彩度・明度を捨てる**(hue のみ使用)。彩度の低い seed も
  一律 46% 彩度の primary になる。
- **無彩色 seed(グレー系)は hue = 0 → 赤いスキームになる**罠がある。
- HSL の明度はトーン(知覚輝度)と一致しないため、hue によってコントラスト比が
  ±変動する(黄系 seed で顕著)。

正確化する場合は `com.google.android.material:material-color-utilities` 相当の HCT
実装を持ち込むことになるが、**「依存ゼロ・コヒーレントなら十分」という現在の設計判断は
妥当**。現行 5 ブランドの seed はどれも高彩度・中間明度で近似が破綻しない。当面は:

1. `KansoBrand` の KDoc に「seed は彩度の高い中間トーンを推奨。無彩色不可」と明記(S)。
2. 無彩色 seed(HSV S < 0.05 など)を検出したらニュートラル一色のスキームに
   フォールバックするガードを入れる(S)。
3. HCT 移行は「WCAG コントラスト保証が必要になった時」の再検討事項として棚上げ。

#### E16. プレビュー規約とスクリーンショットテスト `M`

- `@KansoPreview`: light/dark × fontScale 1.0/1.5 のマルチプレビュー注釈を library に
  同梱。全コンポーネントに付ける。
- スクリーンショットテスト(Roborazzi)を `:kanso` に導入し、**無効化した lint の
  代わりの回帰ゲート**にする。lint 本体は AGP/Kotlin 更新で UAST クラッシュが解消され
  次第 `checkReleaseBuilds = true` に戻す(build.gradle.kts の TODO として明記)。

#### E17. アクセシビリティ方針の明文化 `S`

- 装飾アイコンは `contentDescription = null`(現状の実装は正しい)— これを**規約として
  KDoc に明記**。意味を持つアイコン(E2 バッジ等)は説明必須のパラメータ設計にする。
- インタラクティブ行は 48dp 最小(E4 で保証)。
- `KansoStatusBadge` は色だけに依存しない(必ずテキスト/アイコン併記)— E2 の設計に組込み済み。

#### E18. 配布とバージョニング `M`

現状の git submodule 運用は 4 アプリ規模では正しい。ただし:

- タグ運用(`v0.x.y`)+ CHANGELOG.md を今から始める(submodule でも「どの版に
  固定しているか」が言えるようになる)。
- アプリが 5 個を超える・外部公開する時点で JitPack / maven-publish を再検討。
- Compose BOM 更新は kanso 側で一元管理されている(良い設計)ので、BOM 更新を
  CHANGELOG の第一級イベントとして扱う。

---

## 4. 見送る案(理由付き)

| 案 | 見送る理由 |
|---|---|
| 独自ブランドフォント | プラットフォーム既定が最も読みやすく、フォント資産の管理コストが哲学に反する。`KansoTypography` の差し替え口は既にある |
| ナビゲーションフレームワーク統合(route 型安全化等) | アプリ層の関心。kanso は `navigation-compose` を BOM 整合のため公開するに留める |
| Compose Multiplatform 化 | 全アプリ Android。`android.graphics.Color` 依存(hueOf)の除去等コストの割に受益者ゼロ |
| チャート/グラフ | medcal で欲しくなり得るが、1 アプリのための大型部品はまずアプリ内で育てるべき(2 アプリ目が必要とした時に昇格) |
| デザイントークンの JSON 外部化 | ツールチェーン(Figma 連携等)がない個人フリートでは純オーバーヘッド |

---

## 5. 推奨ロードマップ

| フェーズ | 内容 | 規模感 |
|---|---|---|
| **Phase 1**(次リリース) | E1 セマンティックカラー → E2 バッジ → E3 ダイアログ → E4 設定行 → E5 フィールド拡張 → E6 エラー状態。デモに "Status" セクション追加 | ~1 週間 |
| **Phase 2** | E7 選択 → E8 リフレッシュ → E12 バナー/区切り → E13 スキャフォールド拡張 → E9 スケルトン → E10 シート | ~1 週間 |
| **Phase 3** | E11 適応ナビ、E14 モーション、E16 スクリーンショットテスト、E17/E18 の規約整備 | 継続的 |

Phase 1 だけで CertWatch と LMSA の UI はほぼ kanso 部品のみで組めるようになる。
**E1 → E2 の順序だけは固定**(バッジがセマンティック色に依存)。他は独立して着手可能。

---

## 6. 拡張の前に直しておきたい小さな点(既存コードレビュー)

拡張とは独立に、今のうちに揃えておくと後が楽になるもの:

1. **`KansoListItem`**: アイコンの `Modifier.size(24.dp).padding(end = Kanso.spacing.none)`
   の `padding(end = 0.dp)` は無意味 — 削除(`ListItems.kt:42`)。
2. **`KansoCard`**: 見出しと本文の間隔が `Spacer(Modifier.padding(top = …))` —
   `Spacer(Modifier.height(Kanso.spacing.md))` が意図通り(`Surfaces.kt:51`)。
   結果の寸法は同じだが、padding によるサイズ確保は読み手を誤らせる。
3. **`Spacer` の FQN 呼び出し**が component 各所にある
   (`androidx.compose.foundation.layout.Spacer(…)`)— import に統一。
4. **`KansoBrands.Kanso` と `Lms` の seed が同一**(`#006A60`)。意図的(LMSA が
   デフォルトブランドを名乗る)なら KDoc に明記、そうでなければ LMSA に固有 seed を。
5. **`dynamicColor = true` がデフォルト**であることの再確認: Android 12+ では
   ブランド seed が壁紙色に**常時**負ける。ブランドアイデンティティを優先するなら
   デフォルト `false`(ユーザー設定でオプトイン)が「per-app accent」モデルとは整合的。
   現デモも初期値 `dynamic = false` で起動しており、意図とデフォルトがずれている。

---

*このドキュメントは提案であり、各項目の実装時に個別 PR で API を確定させる。*
