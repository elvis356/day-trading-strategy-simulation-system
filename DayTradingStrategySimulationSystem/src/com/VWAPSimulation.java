package com;

/**
 * 執行 VWAP 價量均線突破策略的子類別
 */
public class VWAPSimulation extends DTSimulation {
	public VWAPSimulation(String symbol, double capital, double riskRatio, double maxDailyLoss, String strategyType, double rrRatio) {
		super(symbol, capital, riskRatio, maxDailyLoss, strategyType, rrRatio);
	}

	@Override
	protected String runStrategy() {
		cumulativePV += getClose() * getVolume();
		cumulativeVol += getVolume();
		if (cumulativeVol == 0) return null;
		double vwap = cumulativePV / cumulativeVol;

		if (getPosition() == 0) {
			// 帶量向上突破 VWAP
			if (getClose() > vwap && getOpen() < vwap) {
				String tradeLog = triggerTrade(1, getClose(), vwap);
				return String.format("【%s - VWAP 突破】當前 VWAP: %.2f，價格帶量向上貫穿！\n", getSymbol(), vwap) + (tradeLog != null ? tradeLog : "");
			}
			// 帶量向下貫穿 VWAP
			else if (getClose() < vwap && getOpen() > vwap) {
				String tradeLog = triggerTrade(-1, getClose(), vwap);
				return String.format("【%s - VWAP 跌破】當前 VWAP: %.2f，價格帶量向下貫穿！\n", getSymbol(), vwap) + (tradeLog != null ? tradeLog : "");
			}
		}
		return null;
	}
}