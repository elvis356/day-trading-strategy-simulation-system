package com;

import java.time.LocalTime;

/**
 * 模擬系統核心模組與控制器類別 (Model + Controller)
 * 包含 KBar（K 線資料結構）、RiskEngine（風控管理）、TradingEngine（交易執行引擎）
 * 以及整合原 Controller 的多策略邏輯與狀態管理
 */
class DTSimulation {
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
	private int barCount = 0;
	/** 紀錄開盤區間突破（ORB）策略中的最高價與最低價邊界 */
	private double orbHigh, orbLow;
	/** 用於計算 VWAP（成交量加權平均價）的累計「價格 × 成交量 (PV)」乘積總和 */
	private double cumulativePV = 0.0;
	/** 用於計算 VWAP 的累計總成交量 */
	private long cumulativeVol = 0;
	/** 紀錄系統是否已經擁有上一根 K 線的暫存資料，供 V 轉等需要比對前後 K 線型態的策略使用 */
	private boolean hasLastBar = false;
	/** 紀錄前一根 K 線的開盤價、收盤價、最低價、最高價，用於型態學比對 */
	private double lastOpen, lastClose, lastLow, lastHigh;

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

		// 3. 根據策略類型執行對應演算法
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
	 * 根據指定策略進行派發處理
	 * @return 策略觸發後的訊號或日誌訊息
	 */
	private String runStrategy() {
		switch (strategyType) {
			case "開盤區間突破 (ORB)":
				return runORB();
			case "均線均值回歸 (Mean Reversion)":
				return runMeanReversion();
			case "VWAP 價量均線突破 (VWAP Breakout/Cross)":
				return runVWAP();
			case "V 轉反彈 (V-Shape Reversal)":
				return runVShape();
			default:
				return "";
		}
	}

	/**
	 * 執行開盤區間突破 (ORB) 策略
	 * 前三根 K 線（共 15 分鐘）建立當日開盤高低價區間，之後價格突破上緣做多、跌破下緣做空
	 * @return 策略觸發後的訊號或日誌訊息
	 */
	private String runORB() {
		// 收集前 3 根 K 線以建立開盤區間 (Opening Range)
		if (barCount < 3) {
			if (barCount == 0) {
				orbHigh = high;
				orbLow = low;
			} else {
				if (high > orbHigh) orbHigh = high;
				if (low < orbLow) orbLow = low;
			}
			barCount++;
			if (barCount == 3) {
				return String.format("【%s - ORB 建立】15 分鐘最高: %.2f, 最低: %.2f", symbol, orbHigh, orbLow);
			}
			return null;
		}
		
		// 區間建立完成後，在空倉狀態下尋找突破機會
		if (position == 0) {
			if (close > orbHigh) {
				// 向上突破，觸發做多
				return triggerTrade(1, close, Math.max(orbLow, close * 0.99));
			} else if (close < orbLow) {
				// 向下跌破，觸發做空
				return triggerTrade(-1, close, Math.min(orbHigh, close * 1.01));
			}
		}
		return null;
	}

	/**
	 * 執行均線均值回歸策略
	 * 當價格過度偏離模擬均線時，假設其將回歸均值，進行逆勢超買做空或超賣做多
	 * @return 策略觸發後的訊號或日誌訊息
	 */
	private String runMeanReversion() {
		double estimatedMA = close * 0.983; // 模擬均線 (假設現價高於均線)
		double estimatedMABear = close * 1.017; // 模擬均線 (假設現價低於均線)

		if (position == 0) {
			// 價格過度高於均線，超買做空
			if ((close - estimatedMA) / estimatedMA > 0.015) {
				String tradeLog = triggerTrade(-1, close, close * 1.01);
				return String.format("【%s - 均值回歸】價格過度偏離均線上方，觸發做空訊號！\n", symbol) + (tradeLog != null ? tradeLog : "");
			}
			// 價格過度低於均線，超賣做多
			else if ((estimatedMABear - close) / estimatedMABear > 0.015) {
				String tradeLog = triggerTrade(1, close, close * 0.99);
				return String.format("【%s - 均值回歸】價格過度偏離均線下方，觸發做多訊號！\n", symbol) + (tradeLog != null ? tradeLog : "");
			}
		}
		return null;
	}

	/**
	 * 執行 VWAP 價量均線突破策略
	 * 動態計算成交量加權平均價（VWAP），當價格帶量向上或向下貫穿 VWAP 時順勢進場
	 * @return 策略觸發後的訊號或日誌訊息
	 */
	private String runVWAP() {
		cumulativePV += close * volume;
		cumulativeVol += volume;
		if (cumulativeVol == 0) return null;
		double vwap = cumulativePV / cumulativeVol;

		if (position == 0) {
			// 帶量向上突破 VWAP
			if (close > vwap && open < vwap) {
				String tradeLog = triggerTrade(1, close, vwap);
				return String.format("【%s - VWAP 突破】當前 VWAP: %.2f，價格帶量向上貫穿！\n", symbol, vwap) + (tradeLog != null ? tradeLog : "");
			}
			// 帶量向下貫穿 VWAP
			else if (close < vwap && open > vwap) {
				String tradeLog = triggerTrade(-1, close, vwap);
				return String.format("【%s - VWAP 跌破】當前 VWAP: %.2f，價格帶量向下貫穿！\n", symbol, vwap) + (tradeLog != null ? tradeLog : "");
			}
		}
		return null;
	}

	/**
	 * 執行 V 轉反彈與倒 V 反轉策略
	 * 透過比對前後兩根 K 線的價格型態（如長黑後出現強勢包覆收高即為 V 轉多頭訊號）
	 * @return 策略觸發後的訊號或日誌訊息
	 */
	private String runVShape() {
		if (hasLastBar && position == 0) {
			// 多方：V 轉反彈 (前一根長黑，當前強勢包覆收高)
			boolean lastIsBigRed = (lastOpen - lastClose) / lastOpen > 0.015;
			boolean currentStrongGreen = close > lastOpen;
			if (lastIsBigRed && currentStrongGreen) {
				String tradeLog = triggerTrade(1, close, lastLow);
				return String.format("【%s - V 轉反彈】前 K 長黑後本 K 強勢包覆，觸發多頭進場！\n", symbol) + (tradeLog != null ? tradeLog : "");
			}

			// 空方：倒 V 反轉 (前一根長紅，當前強勢殺盤收低跌破前開)
			boolean lastIsBigGreen = (lastClose - lastOpen) / lastOpen > 0.015;
			boolean currentStrongRed = close < lastOpen;
			if (lastIsBigGreen && currentStrongRed) {
				String tradeLog = triggerTrade(-1, close, lastHigh);
				return String.format("【%s - 倒V反轉】前 K 長紅後本 K 強勢殺盤，觸發空頭進場！\n", symbol) + (tradeLog != null ? tradeLog : "");
			}
		}
		return null;
	}

	// ===================== 5. 交易執行與狀態管理方法 (Trading Execution & State) =====================
	/**
	 * 根據進場價與停損價計算風險並觸發下單
	 * @param pos 進場方向（1: 多, -1: 空）
	 * @param entry 預定進場價
	 * @param stop 預定停損價
	 * @return 下單成功訊息或 null
	 */
	private String triggerTrade(int pos, double entry, double stop) {
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
		// 修正：計算總損益時必須乘上持倉部位方向與實際股數 (shares)
		double pnl = (price - entryPrice) * position * shares;
		this.dailyAccumulatedPnL += pnl;
		this.position = 0;
		this.entryPrice = 0.0;
		this.stopLoss = 0.0;
		this.takeProfit = 0.0;
		this.shares = 0; // 重設股數
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
	/**
	 * 更新並覆寫當前的 K 線數據物件
	 * @param time K 線時間戳記
	 * @param open 開盤價
	 * @param high 最高價
	 * @param low 最低價
	 * @param close 收盤價
	 * @param volume 成交量
	 */
	public void updateKBar(LocalTime time, double open, double high, double low, double close, long volume) {
		this.time = time;
		this.open = open;
		this.high = high;
		this.low = low;
		this.close = close;
		this.volume = volume;
	}

	/**
	 * 設定當日是否觸發風控熔斷機制
	 * @param circuitBroken 是否熔斷
	 */
	public void setCircuitBroken(boolean circuitBroken) { isCircuitBroken = circuitBroken; }
	
	/**
	 * 取得當日是否已觸發風控熔斷機制
	 * @return 是否熔斷
	 */
	public boolean isCircuitBroken() { return isCircuitBroken; }
	
	/**
	 * 取得當前持倉狀態
	 * @return 持倉狀態（1: 多, -1: 空, 0: 空倉）
	 */
	public int getPosition() { return position; }
	
	/**
	 * 取得當前持倉的實際進場成交價
	 * @return 進場成交價
	 */
	public double getEntryPrice() { return entryPrice; }
	
	/**
	 * 取得系統計算出的停損價
	 * @return 停損價
	 */
	public double getStopLoss() { return stopLoss; }
	
	/**
	 * 取得系統計算出的停利價
	 * @return 停利價
	 */
	public double getTakeProfit() { return takeProfit; }
	
	/**
	 * 取得當日截至目前為止累積的已實現總損益
	 * @return 累計總損益
	 */
	public double getDailyAccumulatedPnL() { return dailyAccumulatedPnL; }

	/**
	 * 取得當前 K 線的時間戳記
	 * @return 時間戳記
	 */
	public LocalTime getTime() { return time; }
	
	/**
	 * 取得當前 K 線的開盤價
	 * @return 開盤價
	 */
	public double getOpen() { return open; }
	
	/**
	 * 取得當前 K 線的最高價
	 * @return 最高價
	 */
	public double getHigh() { return high; }
	
	/**
	 * 取得當前 K 線的最低價
	 * @return 最低價
	 */
	public double getLow() { return low; }
	
	/**
	 * 取得當前 K 線的收盤價
	 * @return 收盤價
	 */
	public double getClose() { return close; }
	
	/**
	 * 取得當前 K 線的成交量
	 * @return 成交量
	 */
	public long getVolume() { return volume; }
}