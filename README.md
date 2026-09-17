# Day Trading Strategy Simulation System

一個以 **Java 11 + Swing** 開發的「當沖多策略與動態風控模擬系統」，用於以 5 分鐘 K 線資料進行策略訊號、部位管理、停損停利與單日風控熔斷的模擬。

> **定位：策略研究與程式交易邏輯驗證工具**
>
> 本專案目前以手動輸入 K 線資料進行模擬，並非即時行情系統，也不直接連接券商下單 API。

---

## 1. 專案目標

本系統的核心目標是將「交易策略」、「部位大小計算」與「風控機制」整合在單一模擬流程中：

1. 設定模擬標的與交易資金。
2. 選擇當沖交易策略。
3. 設定單筆風險比例、風報比與單日最大虧損。
4. 依序輸入 5 分鐘 OHLCV K 線。
5. 根據策略條件產生做多／做空訊號。
6. 根據風險金額與停損距離計算部位大小。
7. 持續監控停損、停利與尾盤強制平倉。
8. 累計已實現損益。
9. 當日虧損達到上限時觸發風控熔斷，禁止新的交易。

---

## 2. 技術規格

| 項目 | 技術 |
|---|---|
| 程式語言 | Java |
| Java 版本 | Java 11 |
| GUI | Java Swing |
| IDE 專案 | Eclipse |
| 建置方式 | Eclipse Java Builder / `javac` |
| 外部套件 | 目前無第三方依賴 |
| 輸入資料 | 5 分鐘 OHLCV K 線 |
| 核心架構 | Model + Controller 整合式設計 |
| 執行平台 | 支援 Java 11 的 Windows / macOS / Linux |

---

## 3. 專案結構

```text
day-trading-strategy-simulation-system/
├─ DayTradingStrategySimulationSystem/
│  ├─ src/
│  │  └─ com/
│  │     ├─ DTSimulation.java
│  │     └─ DTSimulationUI.java
│  ├─ bin/
│  │  └─ com/
│  │     ├─ DTSimulation.class
│  │     └─ DTSimulationUI.class
│  ├─ .classpath
│  ├─ .project
│  ├─ .settings/
│  └─ todo.txt
├─ DTSimulation.jar
└─ README.md
```

### 核心類別

#### `DTSimulation.java`

交易模擬核心，負責：

- K 線資料更新
- 策略派發
- 交易訊號判斷
- 多／空部位管理
- 停損／停利價格計算
- 部位大小計算
- 損益結算
- 當日風控熔斷
- 尾盤強制平倉
- 每日狀態重置

#### `DTSimulationUI.java`

Swing 圖形介面，負責：

- 交易參數輸入
- 策略選擇
- 5 分鐘 K 線資料輸入
- 啟動／初始化模擬引擎
- 即時顯示交易日誌
- 顯示累積損益
- 重置當日資料
- 列印日誌
- 操作說明
- 離開程式

---

## 4. 系統流程

```text
┌──────────────────────┐
│  使用者設定交易參數   │
│ 標的 / 本金 / 策略     │
│ RR / 單筆風險 / 最大虧損│
└──────────┬───────────┘
           │
           ▼
┌──────────────────────┐
│   初始化策略引擎      │
└──────────┬───────────┘
           │
           ▼
┌──────────────────────┐
│ 輸入 5 分鐘 OHLCV K線 │
└──────────┬───────────┘
           │
           ▼
┌──────────────────────┐
│   更新 K 線狀態       │
└──────────┬───────────┘
           │
           ▼
┌──────────────────────┐
│     風控檢核          │
│ 熔斷 / 停損 / 停利     │
│ 尾盤強制平倉           │
└──────────┬───────────┘
           │
      未觸發熔斷
           │
           ▼
┌──────────────────────┐
│      策略判斷         │
│ ORB / Mean Reversion │
│ VWAP / V-Shape       │
└──────────┬───────────┘
           │
       產生訊號
           ▼
┌──────────────────────┐
│     部位大小計算      │
│ 風險金額 ÷ 停損距離   │
└──────────┬───────────┘
           │
           ▼
┌──────────────────────┐
│       建立部位        │
│ Entry / SL / TP / Qty │
└──────────┬───────────┘
           │
           ▼
┌──────────────────────┐
│ 後續 K 線持續監控     │
└──────────┬───────────┘
           │
           ▼
┌──────────────────────┐
│       平倉結算        │
│   計算並累計 PnL      │
└──────────────────────┘
```

---

## 5. 交易參數

### 5.1 模擬標的代號

預設值：

```text
2330
```

目前僅作為模擬標識，不會自動從市場 API 取得行情。

---

### 5.2 當沖本金

預設：

```text
1,000,000 元
```

用於計算單筆交易的風險資金。

---

### 5.3 單筆風險比例

預設：

```text
0.01 = 1%
```

單筆交易允許承擔的風險金額：

```text
Risk Amount = Capital × Risk Ratio
```

例如：

```text
本金 = 1,000,000
單筆風險比例 = 1%

Risk Amount = 1,000,000 × 0.01
            = 10,000 元
```

---

### 5.4 風報比 RR

預設：

```text
2.0
```

停利距離依照停損距離計算：

```text
Risk Distance = |Entry Price - Stop Loss|

Take Profit Distance = Risk Distance × RR
```

多單：

```text
Take Profit = Entry + Risk Distance × RR
```

空單：

```text
Take Profit = Entry - Risk Distance × RR
```

---

### 5.5 當日虧損上限

預設：

```text
10,000 元
```

當：

```text
Daily Accumulated PnL <= -Max Daily Loss
```

系統會：

1. 觸發 `Circuit Breaker`。
2. 鎖定當日新開倉。
3. 後續 K 線不再執行策略進場。

---

## 6. 部位大小計算

系統目前使用風險金額控制部位：

```text
Position Size
= (Capital × Risk Ratio)
  ÷ |Entry Price - Stop Loss|
```

程式實作：

```java
double riskPerShare = Math.abs(entryPrice - stopLossPrice);

int shares = (int) (
    (capital * riskRatio) / riskPerShare
);
```

例如：

```text
本金              = 1,000,000
單筆風險比例       = 1%
單筆最大風險       = 10,000
進場價格           = 900
停損價格           = 890
每股風險           = 10

部位大小 = 10,000 / 10
         = 1,000 股
```

> 注意：目前程式未進一步檢查可用現金、整股限制、融資／融券限制、契約乘數或保證金需求。

---

## 7. 支援的交易策略

### 7.1 開盤區間突破 ORB

**Opening Range Breakout**

系統以前 3 根 5 分鐘 K 線建立開盤區間，共 15 分鐘。

```text
ORB High = 前 3 根 K 線最高價
ORB Low  = 前 3 根 K 線最低價
```

之後：

```text
Close > ORB High → 做多

Close < ORB Low  → 做空
```

停損：

```text
多單：max(ORB Low, Entry × 0.99)
空單：min(ORB High, Entry × 1.01)
```

---

### 7.2 均線均值回歸

**Mean Reversion**

目前版本使用模擬均線／偏離率邏輯，而非真正由歷史 K 線計算移動平均。

目前程式使用：

```text
estimatedMA     = Close × 0.983
estimatedMABear = Close × 1.017
```

當價格被判定為過度偏離時：

```text
偏離上方 → 做空

偏離下方 → 做多
```

目前策略屬於簡化的模擬版本。

---

### 7.3 VWAP 價量均線突破

**VWAP Breakout / Cross**

系統累計：

```text
Cumulative PV = Σ(Close × Volume)
Cumulative Vol = Σ(Volume)

VWAP = Cumulative PV / Cumulative Vol
```

向上穿越：

```text
Open < VWAP
Close > VWAP
```

→ 做多

向下穿越：

```text
Open > VWAP
Close < VWAP
```

→ 做空

---

### 7.4 V 轉反彈／倒 V 反轉

透過比較前一根與目前 K 線型態判斷反轉。

#### V 轉做多

前一根 K 線跌幅超過 1.5%，且目前 K 線收盤突破前一根開盤價：

```text
Previous Bearish > 1.5%
AND
Current Close > Previous Open
```

→ 做多

停損：

```text
Previous Low
```

#### 倒 V 做空

前一根 K 線漲幅超過 1.5%，且目前 K 線收盤跌破前一根開盤價：

```text
Previous Bullish > 1.5%
AND
Current Close < Previous Open
```

→ 做空

停損：

```text
Previous High
```

---

## 8. 風控機制

系統目前的風控檢查順序為：

### ① 當日虧損熔斷

```text
Daily PnL <= -Max Daily Loss
```

觸發後禁止新的交易。

---

### ② 尾盤強制平倉

當模擬時間：

```text
> 13:20
```

若仍有持倉，系統會執行：

```text
13:20 尾盤強制平倉
```

---

### ③ 多單停損／停利

```text
Current Price <= Stop Loss
    → 停損

Current Price >= Take Profit
    → 停利
```

---

### ④ 空單停損／停利

```text
Current Price >= Stop Loss
    → 停損

Current Price <= Take Profit
    → 停利
```

---

## 9. 損益計算

目前未扣除交易成本。

多單：

```text
PnL = (Exit Price - Entry Price) × Shares
```

空單：

```text
PnL = (Exit Price - Entry Price) × (-1) × Shares
```

統一公式：

```text
PnL = (Exit Price - Entry Price)
      × Position Direction
      × Shares
```

其中：

```text
Position Direction
1  = Long
-1 = Short
```

每日累計：

```text
Daily Accumulated PnL
+= Trade PnL
```

---

## 10. GUI 操作流程

### Step 1：設定參數

設定：

- 模擬標的代號
- 當沖本金
- 交易策略
- 當日虧損上限
- 風報比 RR
- 單筆風險比例

---

### Step 2：初始化

按：

```text
初始化策略引擎
```

系統會建立新的 `DTSimulation` 實例，並將模擬時間重設為：

```text
09:05
```

---

### Step 3：輸入 K 線

輸入：

```text
Open
High
Low
Close
Volume
```

例如：

```text
Open   = 900
High   = 905
Low    = 898
Close  = 902
Volume = 1200
```

按：

```text
送出 K 線數據
```

系統會自動將下一根 K 線時間增加 5 分鐘。

---

### Step 4：查看系統日誌

右側監控區會顯示：

- K 線資料
- 策略訊號
- 下單結果
- 進場價
- 停損價
- 停利價
- 部位大小
- 平倉原因
- 單筆損益
- 累計損益
- 風控熔斷

---

### Step 5：重置

按：

```text
重置當日資料
```

會清除：

- 持倉
- Entry
- Stop Loss
- Take Profit
- Shares
- Daily PnL
- Circuit Breaker
- ORB 狀態
- VWAP 累計資料
- 前一根 K 線狀態

但會保留：

- 標的
- 本金
- 風險比例
- 最大虧損
- 策略
- RR

---

## 11. 執行方式

### 方法 A：使用 Eclipse

1. 安裝 JDK 11。
2. 使用 Eclipse 匯入 `DayTradingStrategySimulationSystem`。
3. 確認 Project 使用 Java 11。
4. 執行：

```text
com.DTSimulationUI
```

---

### 方法 B：命令列編譯

進入：

```text
DayTradingStrategySimulationSystem/
```

執行：

```bash
javac -d bin src/com/*.java
```

執行：

```bash
java -cp bin com.DTSimulationUI
```

---

### 方法 C：執行既有 JAR

專案根目錄已提供：

```text
DTSimulation.jar
```

可嘗試：

```bash
java -jar DTSimulation.jar
```

若 JAR 未設定正確的 `Main-Class` manifest，請改用 Eclipse 或 `java -cp` 方式啟動。

---

## 12. 目前架構

目前核心程式採用「Model + Controller 整合」形式：

```text
DTSimulationUI
      │
      │ processKline()
      ▼
DTSimulation
 ├─ K 線狀態
 ├─ 策略引擎
 │   ├─ ORB
 │   ├─ Mean Reversion
 │   ├─ VWAP
 │   └─ V-Shape
 ├─ 交易執行
 ├─ 部位計算
 ├─ 損益計算
 └─ 風控引擎
```

目前 `DTSimulation` 尚未拆分成獨立的：

```text
Strategy
RiskEngine
TradingEngine
Position
KBar
Portfolio
```

後續若要擴充策略數量、資料來源與回測功能，建議逐步進行模組化重構。

---

## 13. 已知限制

目前版本屬於「策略邏輯模擬 Prototype」，具有以下限制：

### 市場資料

- 沒有串接即時行情 API。
- 沒有 CSV 批次匯入。
- K 線目前由使用者手動輸入。
- 沒有歷史資料回測框架。
- 沒有資料品質驗證。

### 交易成本

目前 `closePosition()` 尚未扣除：

- 券商手續費
- 當沖證券交易稅
- 滑價
- 其他交易成本

因此目前 PnL 為：

```text
Gross PnL
```

而非完整的：

```text
Net PnL
```

### 部位限制

目前沒有完整處理：

- 現金可用額度
- 股票整股／零股限制
- 融資
- 融券
- 當沖資格
- 保證金
- 契約乘數
- 漲跌停限制
- 市場交易規則

### K 線內價格路徑

目前停損／停利判斷主要使用送入的 `close` 價格。

因此若同一根 K 線內同時出現：

```text
High >= Take Profit
Low <= Stop Loss
```

目前模型無法完整還原真實市場中的觸發先後順序。

---

## 14. 待開發項目

目前 `todo.txt` 規劃：

### P0：交易成本模組

在平倉時計算：

```text
Net PnL
= Gross PnL
- Buy Fee
- Sell Fee
- Transaction Tax
- Slippage
```

並將交易成本抽象成獨立模組。

---

### P1：資券／當沖資格過濾

針對不同標的加入：

- 當沖資格
- 警示股
- 處置股
- 資券限制

等交易前檢查。

---

### P1：ATR 動態波動調整

目前部分策略使用固定百分比：

```text
1%
1.5%
```

後續可改為：

```text
ATR × multiplier
```

例如：

```text
Stop Distance = ATR(14) × 1.5
```

讓不同波動度的標的使用不同的停損與策略觸發門檻。

---

### P2：新增 VWMP Pullback

新增：

```text
VWMP Pullback
```

策略，作為現有 VWAP Breakout/Cross 的延伸。

---

## 15. 建議後續架構演進

如果本專案要從「課程／Prototype」進一步發展成完整的量化交易研究工具，建議依下列方向演進：

```text
目前
│
├─ Swing UI
├─ 手動 K 線輸入
├─ 4 種策略
└─ 基本風控
     │
     ▼
Phase 1
├─ CSV / JSON K 線匯入
├─ Backtest Engine
├─ Trade Record
├─ Transaction Cost
└─ Slippage Model
     │
     ▼
Phase 2
├─ Strategy Interface
├─ RiskEngine
├─ PositionManager
├─ Portfolio
└─ MarketDataProvider
     │
     ▼
Phase 3
├─ ATR / Indicator Engine
├─ 更多策略
├─ 策略參數最佳化
├─ 回測報告
└─ Equity Curve
     │
     ▼
Phase 4
├─ 即時行情 API
├─ Paper Trading
├─ Broker API
└─ Live Trading Adapter
```

---

## 16. 建議的模組化目標

未來可將目前的單一核心類別拆成：

```text
com/
├─ app/
│  └─ DTSimulationUI.java
│
├─ domain/
│  ├─ KBar.java
│  ├─ Position.java
│  ├─ Trade.java
│  └─ Portfolio.java
│
├─ strategy/
│  ├─ Strategy.java
│  ├─ ORBStrategy.java
│  ├─ MeanReversionStrategy.java
│  ├─ VWAPStrategy.java
│  ├─ VShapeStrategy.java
│  └─ VWMPPullbackStrategy.java
│
├─ risk/
│  ├─ RiskEngine.java
│  ├─ PositionSizer.java
│  └─ CircuitBreaker.java
│
├─ execution/
│  └─ TradingEngine.java
│
├─ indicator/
│  ├─ VWAP.java
│  ├─ ATR.java
│  └─ MovingAverage.java
│
└─ backtest/
   ├─ BacktestEngine.java
   ├─ BacktestResult.java
   └─ PerformanceReport.java
```

核心設計可以改成：

```java
interface Strategy {
    Signal evaluate(MarketContext context);
}
```

如此新增策略時，不需要持續修改 `DTSimulation` 的巨大 `switch`。

---

## 17. 測試建議

目前專案沒有獨立測試框架。

後續至少應建立以下測試：

### 部位計算

```text
本金 1,000,000
風險 1%
Entry 900
Stop 890

Expected:
Position Size = 1,000
```

### 多單損益

```text
Entry = 900
Exit  = 920
Shares = 100

Expected PnL = +2,000
```

### 空單損益

```text
Entry = 900
Exit  = 880
Shares = 100

Expected PnL = +2,000
```

### 風控熔斷

```text
Daily PnL <= -MaxDailyLoss

Expected:
Circuit Breaker = true
```

### 尾盤平倉

```text
Position != 0
Time > 13:20

Expected:
Position = 0
```

---

## 18. 風險聲明

本專案為**程式設計、策略研究與模擬用途**。

模擬結果不代表實際交易績效。真實市場還會受到：

- 手續費
- 證交稅
- 滑價
- 流動性
- 市場深度
- 撮合順序
- 漲跌停
- 交易資格
- 券商風控
- 行情延遲

等因素影響。

在加入真實券商 API 前，應先完成歷史回測、交易成本模型、滑價模型、風控測試與 Paper Trading 驗證。

---

## 19. License

目前專案未在原始碼中指定正式 License。

若預計公開 GitHub 或提供他人使用，建議後續明確選擇：

```text
MIT
Apache-2.0
GPL-3.0
```

其中一種授權條款，並新增正式的 `LICENSE` 檔案。

---

## 20. 開發狀態

**Current Status：Prototype / Development**

目前已具備：

- [x] Java Swing GUI
- [x] 5 分鐘 K 線手動輸入
- [x] ORB 策略
- [x] Mean Reversion 策略
- [x] VWAP Breakout/Cross 策略
- [x] V-Shape Reversal 策略
- [x] 風險比例部位計算
- [x] RR 風報比
- [x] 停損／停利
- [x] 當日損益累計
- [x] 當日虧損熔斷
- [x] 13:20 尾盤強制平倉
- [x] 系統交易日誌
- [x] 當日資料重置

待完成：

- [ ] 交易成本
- [ ] 滑價模型
- [ ] 當沖資格／警示股過濾
- [ ] ATR 動態波動調整
- [ ] VWMP Pullback
- [ ] CSV 歷史資料匯入
- [ ] Backtest Engine
- [ ] 策略介面化
- [ ] 單元測試
- [ ] 回測績效報表
- [ ] 即時行情資料來源
- [ ] Paper Trading
- [ ] 券商 API Adapter

---

## 21. 快速摘要

```text
Project:
Day Trading Strategy Simulation System

Language:
Java 11

GUI:
Swing

Input:
5-minute OHLCV

Strategies:
1. ORB
2. Mean Reversion
3. VWAP Breakout/Cross
4. V-Shape Reversal

Risk Management:
- Per-trade risk ratio
- Risk/Reward ratio
- Stop Loss
- Take Profit
- Daily loss limit
- Circuit breaker
- 13:20 forced liquidation

Current Position:
Prototype / Strategy Logic Simulation

Next Major Milestones:
Transaction Cost
→ Historical Backtest
→ Modular Strategy Architecture
→ ATR Dynamic Risk
→ Paper Trading
→ Broker API Integration
```
