package com;

/**
 * 執行均線均值回歸策略的子類別
 */
public class MeanReversionSimulation extends DTSimulation {
	public MeanReversionSimulation(String symbol, double capital, double riskRatio, double maxDailyLoss, String strategyType, double rrRatio) {
		super(symbol, capital, riskRatio, maxDailyLoss, strategyType, rrRatio);
	}

	@Override
	protected String runStrategy() {
		double estimatedMA = getClose() * 0.983; // 模擬均線 (假設現價高於均線)
		double estimatedMABear = getClose() * 1.017; // 模擬均線 (假設現價低於均線)

		if (getPosition() == 0) {
			// 價格過度高於均線，超買做空
			if ((getClose() - estimatedMA) / estimatedMA > 0.015) {
				String tradeLog = triggerTrade(-1, getClose(), getClose() * 1.01);
				return String.format("【%s - 均值回歸】價格過度偏離均線上方，觸發做空訊號！\n", getSymbol()) + (tradeLog != null ? tradeLog : "");
			}
			// 價格過度低於均線，超賣做多
			else if ((estimatedMABear - getClose()) / estimatedMABear > 0.015) {
				String tradeLog = triggerTrade(1, getClose(), getClose() * 0.99);
				return String.format("【%s - 均值回歸】價格過度偏離均線下方，觸發做多訊號！\n", getSymbol()) + (tradeLog != null ? tradeLog : "");
			}
		}
		return null;
	}
}