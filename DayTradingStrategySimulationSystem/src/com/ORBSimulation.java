package com;

/**
 * 執行開盤區間突破 (ORB) 策略的子類別
 */
public class ORBSimulation extends DTSimulation {
	public ORBSimulation(String symbol, double capital, double riskRatio, double maxDailyLoss, String strategyType, double rrRatio) {
		super(symbol, capital, riskRatio, maxDailyLoss, strategyType, rrRatio);
	}

	@Override
	protected String runStrategy() {
		// 收集前 3 根 K 線以建立開盤區間 (Opening Range)
		if (barCount < 3) {
			if (barCount == 0) {
				orbHigh = getHigh();
				orbLow = getLow();
			} else {
				if (getHigh() > orbHigh) orbHigh = getHigh();
				if (getLow() < orbLow) orbLow = getLow();
			}
			barCount++;
			if (barCount == 3) {
				return String.format("【%s - ORB 建立】15 分鐘最高: %.2f, 最低: %.2f", getSymbol(), orbHigh, orbLow);
			}
			return null;
		}
		
		// 區間建立完成後，在空倉狀態下尋找突破機會
		if (getPosition() == 0) {
			if (getClose() > orbHigh) {
				// 向上突破，觸發做多
				return triggerTrade(1, getClose(), Math.max(orbLow, getClose() * 0.99));
			} else if (getClose() < orbLow) {
				// 向下跌破，觸發做空
				return triggerTrade(-1, getClose(), Math.min(orbHigh, getClose() * 1.01));
			}
		}
		return null;
	}
}