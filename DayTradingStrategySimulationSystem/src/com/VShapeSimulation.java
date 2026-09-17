package com;

/**
 * 執行 V 轉反彈與倒 V 反轉策略的子類別
 */
public class VShapeSimulation extends DTSimulation {
	public VShapeSimulation(String symbol, double capital, double riskRatio, double maxDailyLoss, String strategyType, double rrRatio) {
		super(symbol, capital, riskRatio, maxDailyLoss, strategyType, rrRatio);
	}

	@Override
	protected String runStrategy() {
		if (hasLastBar && getPosition() == 0) {
			// 多方：V 轉反彈 (前一根長黑，當前強勢包覆收高)
			boolean lastIsBigRed = (lastOpen - lastClose) / lastOpen > 0.015;
			boolean currentStrongGreen = getClose() > lastOpen;
			if (lastIsBigRed && currentStrongGreen) {
				String tradeLog = triggerTrade(1, getClose(), lastLow);
				return String.format("【%s - V 轉反彈】前 K 長黑後本 K 強勢包覆，觸發多頭進場！\n", getSymbol()) + (tradeLog != null ? tradeLog : "");
			}

			// 空方：倒 V 反轉 (前一根長紅，當前強勢殺盤收低跌破前開)
			boolean lastIsBigGreen = (lastClose - lastOpen) / lastOpen > 0.015;
			boolean currentStrongRed = getClose() < lastOpen;
			if (lastIsBigGreen && currentStrongRed) {
				String tradeLog = triggerTrade(-1, getClose(), lastHigh);
				return String.format("【%s - 倒V反轉】前 K 長紅後本 K 強勢殺盤，觸發空頭進場！\n", getSymbol()) + (tradeLog != null ? tradeLog : "");
			}
		}
		return null;
	}
}