package com;

import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Dimension;
import java.awt.EventQueue;
import java.awt.FlowLayout;
import java.awt.Font;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.GridLayout;
import java.awt.Insets;
import java.awt.print.PrinterException;
import java.time.LocalTime;

import javax.swing.BorderFactory;
import javax.swing.JButton;
import javax.swing.JComboBox;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JSpinner;
import javax.swing.JTextArea;
import javax.swing.JTextField;
import javax.swing.SpinnerNumberModel;
import javax.swing.SwingConstants;
import javax.swing.border.EmptyBorder;
import javax.swing.border.TitledBorder;

/**
 * 模擬系統圖形介面類別
 */
public class DTSimulationUI extends JFrame {
	/* =================== UI 欄位 ========================== */
    private static final long serialVersionUID = 1L;

    private JTextField stockSymbol; // 商品代號輸入框
    private JComboBox<String> strategy;
    private JTextField capital, maxLoss;
    private JSpinner RRRatio; // 風報比 JSpinner 支援上下調整數字與設定間距
    private JSpinner riskRatio; // 單筆風險比例 JSpinner
    private JTextField openingPrice, highestPrice, lowestPrice, closingPrice, tradingVolume;
    private JTextArea output;
    private JLabel status, accumulatedProfit;

    private int currentHour = 9;   // 記錄當前小時，支援跨小時遞增
    private int currentMinute = 5;

    /* ===================== 模型實例 ===================== */
    // 模擬系統核心模組實例 (Model + Controller)
    private DTSimulation simulation;

    public static void main(String[] args) {
        EventQueue.invokeLater(() -> {
            try {
                DTSimulationUI frame = new DTSimulationUI();
                frame.setVisible(true);
            } catch (Exception e) {
                e.printStackTrace();
            }
        });
    }

    public DTSimulationUI() {
        setTitle("當沖多策略與動態風控模擬系統");
        setDefaultCloseOperation(JFrame.DO_NOTHING_ON_CLOSE);
        addWindowListener(new java.awt.event.WindowAdapter() {
            @Override
            public void windowClosing(java.awt.event.WindowEvent e) {
                confirmAndExit();
            }
        });
        setBounds(100, 100, 1020, 760);

        JPanel contentPane = new JPanel(new BorderLayout(12, 12));
        contentPane.setBorder(new EmptyBorder(12, 12, 12, 12));
        contentPane.setBackground(new Color(245, 247, 250));
        setContentPane(contentPane);

        // 最上方：策略與風控參數設定 Panel 
        JPanel configPanel = new JPanel(new GridLayout(1, 2, 20, 0));
        configPanel.setBackground(Color.WHITE);
        TitledBorder configBorder = BorderFactory.createTitledBorder(
                BorderFactory.createLineBorder(new Color(220, 225, 230), 1), " 策略選擇與風控參數設定 ");
        configBorder.setTitleFont(new Font("Microsoft JhengHei", Font.BOLD, 12));
        configBorder.setTitleColor(new Color(70, 80, 95));
        configPanel.setBorder(BorderFactory.createCompoundBorder(configBorder, new EmptyBorder(8, 8, 8, 8)));

        // 左側參數設定 Panel
        JPanel leftConfigPanel = new JPanel(new GridBagLayout());
        leftConfigPanel.setBackground(Color.WHITE);

        // 右側參數設定 Panel
        JPanel rightConfigPanel = new JPanel(new GridBagLayout());
        rightConfigPanel.setBackground(Color.WHITE);

        // --- 左側欄位 ---
        // 標的代號標籤
        leftConfigPanel.add(new JLabel("模擬標的代號:", SwingConstants.RIGHT), 
            new GridBagConstraints(0, 0, 1, 1, 0.0, 0.0, GridBagConstraints.EAST, GridBagConstraints.HORIZONTAL, new Insets(6, 4, 6, 8), 0, 0));
        
        // 標的代號輸入框
        stockSymbol = new JTextField("2330");
        leftConfigPanel.add(stockSymbol, 
            new GridBagConstraints(1, 0, 1, 1, 1.0, 0.0, GridBagConstraints.CENTER, GridBagConstraints.HORIZONTAL, new Insets(6, 0, 6, 4), 0, 0));

        // 當沖本金標籤
        leftConfigPanel.add(new JLabel("當沖本金(元):", SwingConstants.RIGHT), 
            new GridBagConstraints(0, 1, 1, 1, 0.0, 0.0, GridBagConstraints.EAST, GridBagConstraints.HORIZONTAL, new Insets(6, 4, 6, 8), 0, 0));
        
        // 當沖本金輸入框
        capital = new JTextField("1000000");
        leftConfigPanel.add(capital, 
            new GridBagConstraints(1, 1, 1, 1, 1.0, 0.0, GridBagConstraints.CENTER, GridBagConstraints.HORIZONTAL, new Insets(6, 0, 6, 4), 0, 0));

        // 風報比標籤
        leftConfigPanel.add(new JLabel("設定風報比 (RR):", SwingConstants.RIGHT), 
            new GridBagConstraints(0, 2, 1, 1, 0.0, 0.0, GridBagConstraints.EAST, GridBagConstraints.HORIZONTAL, new Insets(6, 4, 6, 8), 0, 0));
        
        // 風報比 JSpinner（初始化 JSpinner，預設值為 2.0，範圍 0.5 ~ 20.0，每次調整間距為 0.5）
        RRRatio = new JSpinner(new SpinnerNumberModel(2.0, 0.5, 20.0, 0.5));
        leftConfigPanel.add(RRRatio, 
            new GridBagConstraints(1, 2, 1, 1, 1.0, 0.0, GridBagConstraints.CENTER, GridBagConstraints.HORIZONTAL, new Insets(7, 0, 7, 4), 0, 0));

        // 加入一個具有垂直彈性的空白佔位列 (gridy = 4)，將左側欄位完美向上頂至垂直置中
        leftConfigPanel.add(new JLabel(), 
            new GridBagConstraints(0, 4, 2, 1, 0.0, 1.0, GridBagConstraints.CENTER, GridBagConstraints.BOTH, new Insets(0, 0, 0, 0), 0, 0));

        // --- 右側欄位 ---
        // 選擇策略標籤
        rightConfigPanel.add(new JLabel("選擇當沖策略:", SwingConstants.RIGHT), 
            new GridBagConstraints(0, 0, 1, 1, 0.0, 0.0, GridBagConstraints.EAST, GridBagConstraints.HORIZONTAL, new Insets(6, 4, 6, 8), 0, 0));
        
        // 選擇策略下拉選單
        strategy = new JComboBox<>(new String[]{
            "開盤區間突破 (ORB)",
            "均線均值回歸 (Mean Reversion)",
            "VWAP 價量均線突破 (VWAP Breakout/Cross)",
            "V 轉反彈 (V-Shape Reversal)"
        });
        strategy.setFont(new Font("微軟正黑體", Font.PLAIN, 10));
        rightConfigPanel.add(strategy, 
            new GridBagConstraints(1, 0, 1, 1, 1.0, 0.0, GridBagConstraints.CENTER, GridBagConstraints.HORIZONTAL, new Insets(5, 0, 5, 4), 0, 0));

        // 當日虧損上限標籤
        rightConfigPanel.add(new JLabel("當日虧損上限:", SwingConstants.RIGHT), 
            new GridBagConstraints(0, 1, 1, 1, 0.0, 0.0, GridBagConstraints.EAST, GridBagConstraints.HORIZONTAL, new Insets(6, 4, 6, 8), 0, 0));
        
        // 當日虧損上限輸入框
        maxLoss = new JTextField("10000");
        rightConfigPanel.add(maxLoss, 
            new GridBagConstraints(1, 1, 1, 1, 1.0, 0.0, GridBagConstraints.CENTER, GridBagConstraints.HORIZONTAL, new Insets(6, 0, 6, 4), 0, 0));

        // 單筆風險比例標籤
        rightConfigPanel.add(new JLabel("單筆風險比例:", SwingConstants.RIGHT), 
            new GridBagConstraints(0, 2, 1, 1, 0.0, 0.0, GridBagConstraints.EAST, GridBagConstraints.HORIZONTAL, new Insets(6, 4, 6, 8), 0, 0));

        // 單筆風險比例 JSpinner（初始化 JSpinner，預設值為 0.01，範圍 0.001 ~ 1.0，每次調整間距為 0.01）
        riskRatio = new JSpinner(new SpinnerNumberModel(0.01, 0.001, 1.0, 0.01));
        rightConfigPanel.add(riskRatio, 
            new GridBagConstraints(1, 2, 1, 1, 1.0, 0.0, GridBagConstraints.CENTER, GridBagConstraints.HORIZONTAL, new Insets(6, 0, 6, 4), 0, 0));

        // 操作說明按鈕
        JButton helpButton = new JButton("操作說明");
        helpButton.setBackground(new Color(255, 243, 205));
        helpButton.setForeground(new Color(133, 100, 4));
        rightConfigPanel.add(helpButton, 
            new GridBagConstraints(1, 3, 1, 1, 1.0, 0.0, GridBagConstraints.EAST, GridBagConstraints.NONE, new Insets(6, 0, 6, 4), 0, 0));

        // 加入一個具有垂直彈性的空白佔位列，與左側對稱並維持整體垂直置中
        rightConfigPanel.add(new JLabel(), 
            new GridBagConstraints(0, 4, 2, 1, 0.0, 1.0, GridBagConstraints.CENTER, GridBagConstraints.BOTH, new Insets(0, 0, 0, 0), 0, 0));

        configPanel.add(leftConfigPanel);
        configPanel.add(rightConfigPanel);

        contentPane.add(configPanel, BorderLayout.NORTH);

        // 中間主體：左（K線輸入）+ 右（日誌與工具列）
        JPanel mainBodyPanel = new JPanel(new BorderLayout(12, 12));
        mainBodyPanel.setOpaque(false);

        // --- 左側：K 線資料輸入框 Panel ---
        JPanel inputPanel = new JPanel();
        inputPanel.setBackground(Color.WHITE);
        TitledBorder inputBorder = BorderFactory.createTitledBorder(
                BorderFactory.createLineBorder(new Color(220, 225, 230), 1), "5 分鐘 K 線資料輸入 ");
        inputBorder.setTitleFont(new Font("Microsoft JhengHei", Font.BOLD, 12));
        inputBorder.setTitleColor(new Color(70, 80, 95));
        inputPanel.setBorder(BorderFactory.createCompoundBorder(inputBorder, new EmptyBorder(6, 6, 6, 6)));
        inputPanel.setPreferredSize(new Dimension(310, 0));
        inputPanel.setLayout(null);

        JLabel label = new JLabel("開盤價 (Open):", SwingConstants.RIGHT);
        label.setBounds(11, 23, 140, 44);
        inputPanel.add(label);
        openingPrice = new JTextField("900");
        openingPrice.setBounds(159, 34, 140, 23);
        inputPanel.add(openingPrice);

        JLabel label_1 = new JLabel("最高價 (High):", SwingConstants.RIGHT);
        label_1.setBounds(11, 70, 140, 44);
        inputPanel.add(label_1);
        highestPrice = new JTextField("905");
        highestPrice.setBounds(159, 81, 140, 23);
        inputPanel.add(highestPrice);

        JLabel label_2 = new JLabel("最低價 (Low):", SwingConstants.RIGHT);
        label_2.setBounds(11, 117, 140, 44);
        inputPanel.add(label_2);
        lowestPrice = new JTextField("898");
        lowestPrice.setBounds(159, 128, 140, 23);
        inputPanel.add(lowestPrice);

        JLabel label_3 = new JLabel("收盤價 (Close):", SwingConstants.RIGHT);
        label_3.setBounds(11, 164, 140, 44);
        inputPanel.add(label_3);
        closingPrice = new JTextField("902");
        closingPrice.setBounds(159, 175, 140, 23);
        inputPanel.add(closingPrice);

        JLabel label_4 = new JLabel("成交量 (Volume):", SwingConstants.RIGHT);
        label_4.setBounds(11, 211, 140, 44);
        inputPanel.add(label_4);
        tradingVolume = new JTextField("1200");
        tradingVolume.setBounds(159, 222, 140, 23);
        inputPanel.add(tradingVolume);

        mainBodyPanel.add(inputPanel, BorderLayout.WEST);
        
        JButton initialization = new JButton("初始化策略引擎");
        initialization.setBounds(47, 276, 125, 25);
        inputPanel.add(initialization);
        initialization.setBackground(new Color(230, 240, 255));
        
        JButton submit = new JButton("送出 K 線數據");
        submit.setBounds(182, 276, 116, 25);
        inputPanel.add(submit);
        submit.setEnabled(false);
        submit.setBackground(new Color(220, 245, 230));
        
        // 送出按鈕事件
        submit.addActionListener(e -> {
            if (simulation != null && simulation.isCircuitBroken()) {
                JOptionPane.showMessageDialog(this, "當日虧損達上限觸發熔斷，禁止繼續下單！", "風控攔截", JOptionPane.WARNING_MESSAGE);
                return;
            }

            try {
                double open = Double.parseDouble(openingPrice.getText().trim());
                double high = Double.parseDouble(highestPrice.getText().trim());
                double low = Double.parseDouble(lowestPrice.getText().trim());
                double close = Double.parseDouble(closingPrice.getText().trim());
                long volume = Long.parseLong(tradingVolume.getText().trim());

                LocalTime barTime = LocalTime.of(currentHour, currentMinute);

                // 呼叫 DTSimulation 內部處理 K 線與策略運算，並取得日誌結果
                String resultLog = simulation.processKline(barTime, open, high, low, close, volume);
                log(resultLog.trim());

                // 更新畫面上的損益顯示
                updateProfitAndLoss(simulation.getDailyAccumulatedPnL());

                // 支援正確跨小時遞增（例如 09:55 -> 10:00 -> 10:05...）
                currentMinute += 5;
                if (currentMinute >= 60) {
                    currentMinute = 0;
                    currentHour++;
                }

                status.setText(" 系統狀態：盤中監控中... (" + barTime + ")");
            } catch (Exception ex) {
                JOptionPane.showMessageDialog(this, "請檢查 K 線數字格式！", "輸入錯誤", JOptionPane.ERROR_MESSAGE);
            }
        });

        // 初始化按鈕事件
        initialization.addActionListener(e -> {
            try {
                String symbol = stockSymbol.getText().trim();
                if (symbol.isEmpty()) {
                    JOptionPane.showMessageDialog(this, "請輸入模擬標的代號！", "提示", JOptionPane.WARNING_MESSAGE);
                    return;
                }

                String selectedStrategy = (String) strategy.getSelectedItem();
                double capitalDouble = Double.parseDouble(capital.getText().trim());
                double maxLossDouble = Double.parseDouble(maxLoss.getText().trim());
                // 從 JSpinner 取得調整後的數值 (Double 型態)
                double rrRatioDouble = (Double) RRRatio.getValue();
                double riskRatioDouble = (Double) riskRatio.getValue();

                // 初始化模擬核心模組，並將參數傳入管理
                simulation = new DTSimulation(symbol, capitalDouble, riskRatioDouble, maxLossDouble, selectedStrategy, rrRatioDouble);

                currentHour = 9;   // 重設時數
                currentMinute = 5;
                output.setText("");
                log(String.format(">>> 標的【%s】策略【%s】初始化成功！", symbol, selectedStrategy));
                log(String.format(">>> 本金: $%.0f | 風控虧損上限: $%.0f | 風報比: %.1f | 單筆風險比例: %.2f", capitalDouble, maxLossDouble, rrRatioDouble, riskRatioDouble));
                log(">>> 請輸入 09:05 的第一根 5分鐘 K 線...");

                status.setText(" 系統狀態：請輸入第 1 根 5分鐘 K 線 (09:05)");
                submit.setEnabled(true);
            } catch (Exception ex) {
                JOptionPane.showMessageDialog(this, "請檢查輸入參數格式！", "格式錯誤", JOptionPane.ERROR_MESSAGE);
            }
        });

        // --- 右側：監控日誌 Panel（含上方工具列）---
        JPanel logPanel = new JPanel(new BorderLayout(5, 5));
        logPanel.setBackground(Color.WHITE);
        TitledBorder logBorder = BorderFactory.createTitledBorder(
                BorderFactory.createLineBorder(new Color(220, 225, 230), 1), "系統即時監控與風控日誌 ");
        logBorder.setTitleFont(new Font("Microsoft JhengHei", Font.BOLD, 12));
        logBorder.setTitleColor(new Color(70, 80, 95));
        logPanel.setBorder(BorderFactory.createCompoundBorder(logBorder, new EmptyBorder(6, 6, 6, 6)));

        JPanel logToolBar = new JPanel(new FlowLayout(FlowLayout.RIGHT, 8, 0));
        logToolBar.setOpaque(false);

        JButton resetButton = new JButton("重置當日資料");
        resetButton.setBackground(new Color(255, 240, 220));

        JButton print = new JButton("列印日誌");
        print.setBackground(new Color(240, 240, 245));

        JButton clear = new JButton("清除日誌");
        clear.setBackground(new Color(255, 235, 235));

        logToolBar.add(resetButton);
        logToolBar.add(print);
        logToolBar.add(clear);
        logPanel.add(logToolBar, BorderLayout.NORTH);

        output = new JTextArea();
        output.setEditable(false);
        output.setFont(new Font("Monospaced", Font.PLAIN, 13));
        output.setBackground(new Color(250, 250, 250));
        JScrollPane scrollPane = new JScrollPane(output);
        scrollPane.setBorder(BorderFactory.createEmptyBorder(5, 0, 0, 0));

        logPanel.add(scrollPane, BorderLayout.CENTER);
        mainBodyPanel.add(logPanel, BorderLayout.CENTER);

        contentPane.add(mainBodyPanel, BorderLayout.CENTER);

        // 最下方：狀態列與離開按鈕
        JPanel statusPanel = new JPanel(new BorderLayout(10, 0));
        statusPanel.setBackground(Color.WHITE);
        statusPanel.setBorder(BorderFactory.createLineBorder(new Color(220, 225, 230), 1));
        statusPanel.setPreferredSize(new Dimension(0, 48));

        status = new JLabel(" 系統狀態：請先點擊「初始化策略引擎」", SwingConstants.LEFT);
        status.setFont(new Font("Microsoft JhengHei", Font.BOLD, 13));

        JPanel rightBottomPanel = new JPanel(new FlowLayout(FlowLayout.RIGHT, 15, 8));
        rightBottomPanel.setOpaque(false);

        accumulatedProfit = new JLabel(); 
        updateProfitAndLoss(0.0);       // 透過 updateProfitAndLoss 方法正確初始化 0.0 的格式化顯示字串
        accumulatedProfit.setFont(new Font("Microsoft JhengHei", Font.BOLD, 15));
        accumulatedProfit.setForeground(new Color(0, 100, 200));

        JButton exit = new JButton("離開系統");
        exit.setBackground(new Color(255, 220, 220));
        exit.setForeground(new Color(150, 0, 0));
        exit.setFont(new Font("Microsoft JhengHei", Font.BOLD, 12));

        rightBottomPanel.add(accumulatedProfit);
        rightBottomPanel.add(exit);

        statusPanel.add(status, BorderLayout.CENTER);
        statusPanel.add(rightBottomPanel, BorderLayout.EAST);

        contentPane.add(statusPanel, BorderLayout.SOUTH);

        /***************************************************** events *****************************************************/
        // 「操作說明」按鈕事件
        helpButton.addActionListener(e -> {
            String helpMessage = 
                "【當沖模擬應用程式 - 操作說明】\n\n" +
                "1. 參數設定詳解（策略與風控核心）：\n" +
                "   - 模擬標的代號：輸入您欲進行當沖回測或模擬的商品代碼（例如台積電股票代號「2330」）。\n" +
                "   - 選擇當沖策略：系統會依該策略規則進行多空訊號判斷。\n" +
                "   - 當沖本金：設定本次模擬交易的初始總資金水位（例如 $1,000,000 元），作為後續部位動態計算與損益結算的基礎。\n" +
                "   - 當日虧損上限：設定單日容許的最大累計虧損金額。當損益觸及此閾值時，系統會立即觸發「風控熔斷機制」，強制鎖定當日所有新開倉交易。\n" +
                "   - 設定風報比 (Risk/Reward Ratio, RR)：\n" +
                "     * 意義：代表您期望「每一單位承擔的風險」能換取「幾倍的潛在利潤報酬」。\n" +
                "     * 計算與應用：若設定為 2.0，代表當系統計算出進場點到停損點的風險距離為 10 元時，系統會自動將停利目標價設在距離進場價 20 元的位置（即 1:2 的獲利目標）。\n" +
                "     * 建議：高勝率策略可設定 1.5 ~ 2.0，趨勢突破策略則建議設在 2.0 ~ 3.0 以上以維持良好的期望值。\n" +
                "   - 單筆風險比例 (Risk Ratio per Trade)：\n" +
                "     * 意義：代表在每一次獨立的進場交易中，您願意拿「總本金的多少比例」來承擔被停損的極限風險（資金控管的核心）。\n" +
                "     * 計算與應用：\n" + 
                "        ．若本金為 $1,000,000 元且設定風險比例為 0.01（即 1%），則單筆交易所容許的最大虧損金額為 $10,000 元。\n" + 
                "        ．系統會自動依據「單筆風險金額 ÷ 每股/每口停損價差」來動態計算出該筆交易所應配置的最佳買賣股數（或口數），落實嚴格的部位管理。\n\n" +
                "2. 初始化系統：\n" +
                "   - 參數確認無誤後，點擊「初始化策略引擎」以重置並載入設定狀態。\n\n" +
                "3. 輸入 K 線進行模擬：\n" +
                "   - 依序手動輸入每隔 5 分鐘的開盤價、最高價、最低價、收盤價與成交量，並點擊「送出 K 線數據」。\n\n" +
                "4. 模擬風控與自動平倉機制：\n" +
                "   - 系統會於盤中持續監控：若價格觸及停損或停利價位會自動執行平倉。\n" +
                "   - 若模擬時間到達 13:20，系統將自動觸發「尾盤強制平倉」以符合當沖不留倉原則。\n\n" +
                "5. 檢視與列印：\n" +
                "   - 實時監控面板會完整呈現各筆成交、損益與風控日誌，亦可隨時點擊「列印日誌」輸出紀錄。";
            
            JOptionPane.showMessageDialog(
                this, 
                helpMessage, 
                "使用說明", 
                JOptionPane.INFORMATION_MESSAGE
            );
        });

        // 「重置當日資料」按鈕事件
        resetButton.addActionListener(e -> {
            if (simulation == null) {
                JOptionPane.showMessageDialog(this, "策略引擎尚未初始化！", "提示", JOptionPane.WARNING_MESSAGE);
                return;
            }
            
            Object[] options = {"是", "否"};
            int choice = JOptionPane.showOptionDialog(
                    this,
                    "確定要重置當日的累積損益、持倉與策略 K 線狀態嗎？",
                    "重置確認",
                    JOptionPane.YES_NO_OPTION,
                    JOptionPane.QUESTION_MESSAGE,
                    null,
                    options,
                    options[1]
            );
            
            if (choice == 0) { 
                simulation.resetDailyData();
                
                currentHour = 9;
                currentMinute = 5;
                
                updateProfitAndLoss(0.0);
                
                openingPrice.setText("900");
                highestPrice.setText("905");
                lowestPrice.setText("898");
                closingPrice.setText("902");
                tradingVolume.setText("1200");
                
                status.setText(" 系統狀態：當日資料已重置，請輸入第 1 根 5分鐘 K 線 (09:05)");
                
                log(">>> 【系統重置】當日累積損益、持倉與策略 K 線狀態已清除，輸入框已恢復預設值，可重新開始模擬。");
            }
        });

        // 列印按鈕事件
        print.addActionListener(e -> {
            if (output.getText().trim().isEmpty()) {
                JOptionPane.showMessageDialog(this, "目前沒有可列印的日誌內容！", "提示", JOptionPane.INFORMATION_MESSAGE);
                return;
            }
            try {
                boolean complete = output.print(null, null, true, null, null, true);
                if (complete) {
                    JOptionPane.showMessageDialog(this, "列印作業完成！", "成功", JOptionPane.INFORMATION_MESSAGE);
                }
            } catch (PrinterException ex) {
                JOptionPane.showMessageDialog(this, "列印失敗：" + ex.getMessage(), "錯誤", JOptionPane.ERROR_MESSAGE);
            }
        });

        // 清除按鈕事件
        clear.addActionListener(e -> {
            output.setText("");
            log(">>> 日誌已清空。");
        });

        // 離開按鈕事件
        exit.addActionListener(e -> confirmAndExit());
    }
    
    /* ===================== UI 自定義方法 ===================== */
    // 點選離開前，顯示確認視窗
    private void confirmAndExit() {
        int choice = JOptionPane.showConfirmDialog(
                this,
                "確定要離開當沖模擬系統嗎？",
                "確認離開",
                JOptionPane.YES_NO_OPTION,
                JOptionPane.QUESTION_MESSAGE
        );
        if (choice == JOptionPane.YES_OPTION) {
            System.exit(0);
        }
    }

    // output 每行末尾加 "\n"，實現印出時自動換行效果
    public void log(String msg) {
        if (msg == null || msg.isEmpty()) return;
        output.append(msg + "\n");
        output.setCaretPosition(output.getDocument().getLength());
    }

    // 更新當日累計損益資料
    public void updateProfitAndLoss(double pnl) {
        accumulatedProfit.setText(String.format("當日累計損益: $%.2f", pnl));
        accumulatedProfit.setForeground(pnl < 0 ? new Color(200, 0, 0) : new Color(0, 140, 60));
    }
}