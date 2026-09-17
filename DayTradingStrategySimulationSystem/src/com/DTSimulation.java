package com;

import java.time.LocalTime;

/**
 * 模擬系統核心模組與控制器抽象類別 (Abstract Model + Controller)
 * 包含 KBar（K 線資料結構）、RiskEngine（風控管理）、TradingEngine（交易執行引擎）
 * 以及採用繼承與覆寫設計的多策略架構基底
 */
public abstract class DTSimulation {
	// ===================== 欄位宣告 (Fields) =====================
	// 1. 基本設定與參數欄位 (初始化時決定)
	/** 商品代號（例如：台指期、特定股票代碼等） */
	private String symbol;
	/** 交易帳戶的總本金 */
	private double capital;
	/** 單筆交易所能承受的風險資金比例（例如 0.01 代表願意承受 1% 的本金風險） */
	private double riskRatio;
	/** 當日容許的最大累計虧損金額上限（觸發熔斷的閾值） */
	private double maxDailyLoss;
	/** 目標風險報酬比（Risk/Reward Ratio，例如 2.0 代表賺取風險兩倍的利潤） */
	private double rrRatio;
	/** 使用中的當沖策略類型名稱 */
	private String strategyType;

	// 2. 當前 K 線即時資料欄位 (每次接收新 K 線時更新)
	/** 當前 K 線的時間戳記 */
	private LocalTime time;
	/** 當前 K 線的開盤價、最高價、最低價、收盤價 */
	private double open, high, low, close;
	/** 當前 K 線的成交量 */
	private long volume;

	// 3. 交易部位與結算狀態欄位 (運行過程中動態變動)
	/** 當前持倉狀態：1 代表持多單（Long）、-1 代表持空單（Short）、0 代表空倉（Flat） */
	private int position = 0; 
	/** 紀錄當前持倉的實際進場成交價、系統計算出的停損價、系統計算出的停利價 */
	private double entryPrice = 0.0, stopLoss = 0.0, takeProfit = 0.0;
	/** 紀錄當前持倉的實際成交股數/口數 */
	private int shares = 0;
	/** 紀錄當日截至目前為止累積的已實現總損益（PnL） */
	private double dailyAccumulatedPnL = 0.0;
	/** 標記當日是否已觸發風控熔斷機制（若為 true 則當日禁止再進行任何新開倉交易） */
	private boolean isCircuitBroken = false;

	// 4. 策略運作暫存狀態欄位 (輔助各策略進行計算與型態比對)
	/** 計算已接收的 K 線根數（常應用於開盤初期計算特定區間，如前 3 根 5 分鐘 K 線即前 15 分鐘） */
	protected int barCount = 0;
	/** 紀錄開盤區間突破（ORB）策略中的最高價與最低價邊界 */
	protected double orbHigh, orbLow;
	/** 用於計算 VWAP（成交量加權平均價）的累計「價格 × 成交量 (PV)」乘積總和 */
	protected double cumulativePV = 0.0;
	/** 用於計算 VWAP 的累計總成交量 */
	protected long cumulativeVol = 0;
	/** 紀錄系統是否已經擁有上一根 K 線的暫存資料，供 V 轉等需要比對前後 K 線型態的策略使用 */
	protected boolean hasLastBar = false;
	/** 紀錄前一根 K 線的開盤價、收盤價、最低價、最高價，用於型態學比對 */
	protected double lastOpen, lastClose, lastLow, lastHigh;

	// ===================== 1. 建構子 (Constructor) =====================
	/**
	 * 初始化模擬系統核心，設定商品基本面參數、資金水位與交易策略偏好
	 * @param symbol 商品代號
	 * @param capital 初始本金
	 * @param riskRatio 風險承受比例
	 * @param maxDailyLoss 單日最大容許虧損金額
	 * @param strategyType 交易策略名稱
	 * @param rrRatio 風報比設定
	 */
	public DTSimulation(String symbol, double capital, double riskRatio, double maxDailyLoss, String strategyType, double rrRatio) {
		this.symbol = symbol;
		this.capital = capital;
		this.riskRatio = riskRatio;
		this.maxDailyLoss = maxDailyLoss;
		this.strategyType = strategyType;
		this.rrRatio = rrRatio;
	}

	// ===================== 2. 核心業務邏輯與流程控制 (Core Business Logic) =====================
	/**
	 * 接收新 K 線並更新資料，接著依序執行風控檢核與策略判斷
	 * @param barTime 5分鐘 K 線時間
	 * @param open 開盤價
	 * @param high 最高價
	 * @param low 最低價
	 * @param close 收盤價
	 * @param volume 成交量
	 * @return 執行過程中的日誌訊息字串（供外部 UI 顯示）
	 */
	public String processKline(LocalTime barTime, double open, double high, double low, double close, long volume) {
		StringBuilder logBuilder = new StringBuilder();
		
		// 1. 將 K 線資料寫入核心模組
		updateKBar(barTime, open, high, low, close, volume);
		logBuilder.append(String.format("收到 [%s] K線 [%s] 開:%.2f 高:%.2f 低:%.2f 收:%.2f 量:%d\n", symbol, barTime, open, high, low, close, volume));

		// 2. 先進行風控檢核
		String enforceMsg = inspectAndEnforce(close, barTime);
		if (enforceMsg != null) {
			logBuilder.append(enforceMsg).append("\n");
		}

		if (isCircuitBroken) {
			return logBuilder.toString();
		}

		// 3. 根據策略類型執行對應演算法 (多型呼叫子類別覆寫的方法)
		String strategyMsg = runStrategy();
		if (strategyMsg != null && !strategyMsg.isEmpty()) {
			logBuilder.append(strategyMsg).append("\n");
		}

		// 4. 記錄本根 K 線的必要資訊，供下一根 K 線判斷 V 轉策略使用
		hasLastBar = true;
		lastOpen = open;
		lastClose = close;
		lastLow = low;
		lastHigh = high;

		return logBuilder.toString();
	}

	// ===================== 3. 風控檢核與管理方法 (Risk Management) =====================
	/**
	 * 檢查並執行風控規則（如熔斷、尾盤強制平倉、停利停損）
	 * @param currentPrice 當前價格
	 * @param currentTime 當前時間
	 * @return 回傳字串訊息供呼叫端記錄日誌，無事件則回傳 null
	 */
	public String inspectAndEnforce(double currentPrice, LocalTime currentTime) {
		// 1. 檢核當日累計虧損是否觸發熔斷機制
		if (dailyAccumulatedPnL <= -maxDailyLoss) {
			if (!isCircuitBroken) {
				isCircuitBroken = true;
				return String.format("【%s 風控熔斷】當日虧損 (%.2f) 已達上限 (%.2f)，鎖定交易！", symbol, dailyAccumulatedPnL, maxDailyLoss);
			}
			return null;
		}

		if (position == 0) return null;

		// 2. 檢核是否到達每日 13:20 當沖收盤強制平倉時間
		if (currentTime.isAfter(LocalTime.of(13, 20))) {
			return recordAndClose("13:20 尾盤強制平倉", currentPrice);
		}

		// 3. 檢核現有持倉是否觸及停損或停利價位
		if (position == 1) {
			if (currentPrice <= stopLoss)
				return recordAndClose("觸及多單停損", currentPrice);
			else if (currentPrice >= takeProfit)
				return recordAndClose("觸及多單停利", currentPrice);
		} else if (position == -1) {
			if (currentPrice >= stopLoss)
				return recordAndClose("觸及空單停損", currentPrice);
			else if (currentPrice <= takeProfit)
				return recordAndClose("觸及空單停利", currentPrice);
		}
		return null;
	}

	// ===================== 4. 策略執行方法 (Strategies) =====================
	/**
	 * 抽象策略執行方法，由各具體策略子類別去覆寫 (Override) 實作
	 * @return 策略觸發後的訊號或日誌訊息
	 */
	protected abstract String runStrategy();

	// ===================== 5. 交易執行與狀態管理方法 (Trading Execution & State) =====================
	/**
	 * 根據進場價與停損價計算風險並觸發下單
	 * @param pos 進場方向（1: 多, -1: 空）
	 * @param entry 預定進場價
	 * @param stop 預定停損價
	 * @return 下單成功訊息或 null
	 */
	protected String triggerTrade(int pos, double entry, double stop) {
		double risk = Math.abs(entry - stop);
		double calculatedTakeProfit = (pos == 1) ? entry + (risk * rrRatio) : entry - (risk * rrRatio);
		int shares = calculatePositionSize(entry, stop);
		if (shares > 0) {
			openPosition(pos, entry, stop, calculatedTakeProfit, shares);
			String type = (pos == 1) ? "BUY (做多)" : "SHORT (做空)";
			return String.format("【%s 下單成功】%s | 進場價: %.2f | 停損價: %.2f | 停利價: %.2f | 股數: %d", symbol, type, entry, stop, calculatedTakeProfit, shares);
		}
		return null;
	}

	/**
	 * 根據風險金額與每股停損價差，計算當筆交易應配置的部位大小（股數/口數）
	 * @param entryPrice 進場價
	 * @param stopLossPrice 停損價
	 * @return 計算出的部位大小（股數/口數）
	 */
	public int calculatePositionSize(double entryPrice, double stopLossPrice) {
		double riskPerShare = Math.abs(entryPrice - stopLossPrice);
		if (riskPerShare == 0.0) return 0;
		return (int) ((this.capital * this.riskRatio) / riskPerShare);
	}

	/**
	 * 開立新倉位並記錄相關交易設定
	 * @param newPosition 新持倉狀態（1: 多, -1: 空）
	 * @param price 進場價
	 * @param stopLoss 停損價
	 * @param takeProfit 停利價
	 * @param shares 股數/口數
	 */
	public void openPosition(int newPosition, double price, double stopLoss, double takeProfit, int shares) {
		this.position = newPosition;
		this.entryPrice = price;
		this.stopLoss = stopLoss;
		this.takeProfit = takeProfit;
		this.shares = shares; // 紀錄當前持倉股數
	}

	/**
	 * 內部輔助方法：記錄平倉原因與價格，進行結算並回傳格式化日誌
	 * @param reason 平倉原因
	 * @param exitPrice 平倉價格
	 * @return 格式化日誌訊息
	 */
	private String recordAndClose(String reason, double exitPrice) {
		double tradePnL = closePosition(reason, exitPrice);
		return String.format("【%s 平倉觸發】(%s) | 平倉價: %.2f | 本筆損益: %.2f\n【帳戶結算】當前累計總損益: %.2f", symbol, reason, exitPrice, tradePnL, dailyAccumulatedPnL);
	}

	/**
	 * 關閉現有倉位、計算並累計本筆交易損益，同時重設持倉相關欄位
	 * @param reason 平倉原因
	 * @param price 平倉價格
	 * @return 本筆交易實現損益
	 */
	public double closePosition(String reason, double price) {
		double pnl = (price - entryPrice) * position * shares;
		this.dailyAccumulatedPnL += pnl;
		this.position = 0;
		this.entryPrice = 0.0;
		this.stopLoss = 0.0;
		this.takeProfit = 0.0;
		this.shares = 0;
		return pnl;
	}

	/**
	 * 重置當日當沖累積資料（保留基本設定參數，如本金、風控閾值、策略等）
	 */
	public void resetDailyData() {
		this.position = 0;
		this.entryPrice = 0.0;
		this.stopLoss = 0.0;
		this.takeProfit = 0.0;
		this.shares = 0;
		this.dailyAccumulatedPnL = 0.0;
		this.isCircuitBroken = false;
		this.barCount = 0;
		this.orbHigh = 0.0;
		this.orbLow = 0.0;
		this.cumulativePV = 0.0;
		this.cumulativeVol = 0;
		this.hasLastBar = false;
		this.lastOpen = 0.0;
		this.lastClose = 0.0;
		this.lastLow = 0.0;
		this.lastHigh = 0.0;
	}

	// ===================== 6. 資料存取與狀態方法 (Getters & Setters) =====================
	public void updateKBar(LocalTime time, double open, double high, double low, double close, long volume) {
		this.time = time;
		this.open = open;
		this.high = high;
		this.low = low;
		this.close = close;
		this.volume = volume;
	}

	public void setCircuitBroken(boolean circuitBroken) { isCircuitBroken = circuitBroken; }
	public boolean isCircuitBroken() { return isCircuitBroken; }
	public int getPosition() { return position; }
	public double getEntryPrice() { return entryPrice; }
	public double getStopLoss() { return stopLoss; }
	public double getTakeProfit() { return takeProfit; }
	public double getDailyAccumulatedPnL() { return dailyAccumulatedPnL; }
	public LocalTime getTime() { return time; }
	public double getOpen() { return open; }
	public double getHigh() { return high; }
	public double getLow() { return low; }
	public double getClose() { return close; }
	public long getVolume() { return volume; }
	public String getSymbol() { return symbol; }
}