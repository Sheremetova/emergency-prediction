package ru.spbstu.dis.ui.emergency;

import com.google.common.net.HostAndPort;
import com.typesafe.config.Config;
import org.jfree.chart.plot.XYPlot;
import org.jfree.chart.renderer.xy.XYLineAndShapeRenderer;
import org.jfree.data.time.Millisecond;
import org.slf4j.LoggerFactory;
import ru.spbstu.dis.ConfigProvider;
import ru.spbstu.dis.opc.client.api.OpcClientApiFactory;
import ru.spbstu.dis.opc.client.api.opc.access.*;
import javax.swing.*;
import java.awt.*;
import java.util.*;
import java.util.logging.*;

public class EmergencyPredictionWindow {
  private static final org.slf4j.Logger LOGGER = LoggerFactory
      .getLogger(EmergencyPredictionWindow.class);

  private final OpcAccessApi opcAccessApi;

  static double temperatureGrowthVal;

  static double overflowRiskVal;

  static double tankOverheatClosenessValue;

  public final static Double MAX_TEMPERATURE = 28d;

  static DecisionSupportList decisionSupportList;

  static ArrayList notifications = new ArrayList();

  static String reactorTemperature = Tag.TAG_TO_ID_MAPPING.get(Tag.REACTOR_Controlled_TEMPERATURE);

  static String downstream = Tag.TAG_TO_ID_MAPPING.get(Tag.REACTOR_DOWNSTREAM_ON);

  static String reactorTemperatureSensor = Tag.TAG_TO_ID_MAPPING
      .get(Tag.REACTOR_Current_Process_Temperature);

  static String reactorCooler = Tag.TAG_TO_ID_MAPPING.get(Tag.REACTOR_ControlPanel_Mixing_on);

  static String reactorHeater = Tag.TAG_TO_ID_MAPPING
      .get(Tag.REACTOR_ControlPanel_mixing_pump_P201_on);

  static String mixerValve1 = Tag.TAG_TO_ID_MAPPING.get(Tag.MIX_valve_V201_ToMainTank_on);

  static String mixerValve2 = Tag.TAG_TO_ID_MAPPING.get(Tag.MIX_valve_V202_ToMainTank_on);

  static String mixerValve3 = Tag.TAG_TO_ID_MAPPING.get(Tag.MIX_valve_V203_ToMainTank_on);

  static String mixerSpeed = Tag.TAG_TO_ID_MAPPING.get(Tag.MIX_ControlPanel_FLOW_SPEED);

  static String mixerRealSpeed = Tag.TAG_TO_ID_MAPPING.get(Tag.MIX_TANK_MAN_FLOW_SPEED);

  static String mixerValveSensor1 = Tag.TAG_TO_ID_MAPPING.get(Tag.MIX_valve_V201_ToMainTank_SENSOR);

  static String mixerValveSensor2 = Tag.TAG_TO_ID_MAPPING.get(Tag.MIX_valve_V202_ToMainTank_SENSOR);

  static String mixerValveSensor3 = Tag.TAG_TO_ID_MAPPING.get(Tag.MIX_valve_V203_ToMainTank_SENSOR);

  static String mixerToMaintTankPump = Tag.TAG_TO_ID_MAPPING
      .get(Tag.MIX_ControlPanel_PumpToMainTank_P201_on);

  static String mixerTopWaterLvlSensor = Tag.TAG_TO_ID_MAPPING.get(Tag.MIX_tank_B204_top);

  static String mixerBottomWaterLvlSensor = Tag.TAG_TO_ID_MAPPING
      .get(Tag.MIX_tank_B204_water_bottom_level_sensor);

  static String filter_TP_1M6 = Tag.TAG_TO_ID_MAPPING.get(Tag
                                                            .FILT_ControlPanel_downstream_valve_V103_on);
  static String filter_TP_1M7 = Tag.TAG_TO_ID_MAPPING.get(Tag
                                                              .FILT_ControlPanel_WARING);

  static String filter_p101 = Tag.TAG_TO_ID_MAPPING.get(Tag
      .FILT_pump_101_on);

  static String filter_p102 = Tag.TAG_TO_ID_MAPPING.get(Tag
      .FILT_pump_102_on);

  static double filter_fake_risk_value = 0d;

  static boolean filter_fake_active_flag = true;

  public EmergencyPredictionWindow(final OpcAccessApi opcAccessApi) {
    this.opcAccessApi = opcAccessApi;
  }

  public static void main(String[] args) {

    try {
      for (UIManager.LookAndFeelInfo info : UIManager.getInstalledLookAndFeels()) {
        if ("Nimbus".equals(info.getName())) {
          UIManager.setLookAndFeel(info.getClassName());
          break;
        }
      }
    } catch (Exception e) {
      // If Nimbus is not available, you can set the GUI to another look and feel.
    }
    EmergencyPredictionWindow emergencyPredictionWindow =
        new EmergencyPredictionWindow(createOpcApi());
    emergencyPredictionWindow.initGrowthValueChart();
    emergencyPredictionWindow.initRiskValueChart();
    emergencyPredictionWindow.initThermometerChart();
    emergencyPredictionWindow.initMeterChart();
    emergencyPredictionWindow.initClosenessValueChart();
    SwingUtilities.invokeLater(
        () -> emergencyPredictionWindow.decisionSupportList = new DecisionSupportList(
            "Прогноз развития НС"));
    emergencyPredictionWindow.runSimulation();
  }

  private static OpcAccessApi createOpcApi() {
    final Config config = new ConfigProvider().get().resolve();
    final Config opcAccessApiConf = config.getConfig("http.opc.client");
    final String host = opcAccessApiConf.getString("host");
    final int port = opcAccessApiConf.getInt("port");
    final HostAndPort hostAndPort = HostAndPort.fromParts(host, port);
    LOGGER.warn("Opc access api uses {} to connect", hostAndPort);
    return OpcClientApiFactory.createOpcAccessApi(hostAndPort);
  }

  private void initThermometerChart() {
    final Thermometer demo = new Thermometer("Температура реактора");

    Thread th = new Thread(() -> {
      while (true) {
        demo.setValue(tankOverheatClosenessValue);
        demo.getDataset().setValue(tankOverheatClosenessValue);
        try {
          Thread.sleep(1000);
        } catch (InterruptedException e) {
          e.printStackTrace();
        }
      }
    });
    th.start();
    final JPanel titlePanel = new JPanel(new FlowLayout());
    titlePanel.add(demo.getChartPanel());
    closenessChartFrame.add(titlePanel);
  }

  private void initMeterChart() {
    final MeterChart demo = new MeterChart("Вероятность НС на станциях");

    Thread th = new Thread(() -> {
      while (true) {
//        double v1 = (MAX_TEMPERATURE - temperatureGrowthVal) / MAX_TEMPERATURE;
//        double v = (MAX_FLOW_SPEED - overflowRiskVal) / MAX_FLOW_SPEED;
//        double max = v1 > v ? v1 : v;
        double max = filter_fake_risk_value;
        demo.setValue(max);

        demo.getDataset().setValue(max);
        try {
          Thread.sleep(1000);
        } catch (InterruptedException e) {
          e.printStackTrace();
        }
      }
    });
    th.start();
    final JPanel titlePanel = new JPanel(new FlowLayout());
    titlePanel.add(demo.getChartPanel());
    closenessChartFrame.add(titlePanel);
  }

  private void initClosenessValueChart() {

    Thread th = new Thread(() -> {
      while (true) {
        closenessChartFrame.setLastValue((MAX_FLOW_SPEED - overflowRiskVal) / MAX_FLOW_SPEED);
        try {
          Thread.sleep(1000);
        } catch (InterruptedException e) {
          e.printStackTrace();
        }
      }
    });
    th.start();
    XYPlot plot = (XYPlot) closenessChartFrame.getChartPanel().getChart().getPlot();
    XYLineAndShapeRenderer renderer0 = new XYLineAndShapeRenderer();
    renderer0.setBaseShapesVisible(false);
    XYLineAndShapeRenderer renderer1 = new XYLineAndShapeRenderer();
    renderer1.setBaseShapesVisible(false);
    XYLineAndShapeRenderer renderer2 = new XYLineAndShapeRenderer();
    renderer2.setBaseShapesVisible(false);
    plot.setRenderer(0, renderer0);
    plot.setRenderer(1, renderer1);
    plot.setRenderer(2, renderer2);
    plot.getRendererForDataset(plot.getDataset(0)).setSeriesPaint(0, Color.green);
    plot.getRendererForDataset(plot.getDataset(1)).setSeriesPaint(0, Color.lightGray);
    plot.getRendererForDataset(plot.getDataset(2)).setSeriesPaint(0, Color.red);
    plot.getRenderer().setSeriesPaint(0, Color.green);
    plot.getRenderer().setSeriesPaint(1, Color.red);
    plot.getRenderer().setSeriesPaint(2, Color.blue);

    SwingUtilities.invokeLater(() -> {
      closenessChartFrame.addButton();
      closenessChartFrame.pack();
      closenessChartFrame.setVisible(true);
    });
  }

  private void initGrowthValueChart() {
    final DynamicDataChart demo = new DynamicDataChart("Вероятность перегрева");

    Thread th = new Thread(() -> {
      while (true) {
        //demo.setLastValue((MAX_TEMPERATURE - temperatureGrowthVal) / MAX_TEMPERATURE);
        demo.setLastValue(1.1);
        final Millisecond now = new Millisecond();
        System.out.println("Now = " + now.toString());
        demo.getSeries().add(new Millisecond(), demo.getLastValue());
        try {
          Thread.sleep(1000);
        } catch (InterruptedException e) {
          e.printStackTrace();
        }
      }
    });
    th.start();

    XYPlot plot = (XYPlot) closenessChartFrame.getChartPanel().getChart().getPlot();
    plot.setDataset(plot.getDatasetCount(),
                    ((XYPlot) demo.getChartPanel().getChart().getPlot()).getDataset(0));
  }

  private void initRiskValueChart() {
    final DynamicDataChart demo = new DynamicDataChart(
        "Вероятность засора фильтра станции фильтрации");

    Thread th = new Thread(() -> {
      while (true) {
        if (filter_fake_active_flag) {
          filter_fake_risk_value += 0.1 * new Random().nextDouble();
        }
        demo.setLastValue(filter_fake_risk_value);
        final Millisecond now = new Millisecond();
        System.out.println("Now = " + now.toString());
        demo.getSeries().add(new Millisecond(), demo.getLastValue());
        try {
          Thread.sleep(1000);
        } catch (InterruptedException e) {
          e.printStackTrace();
        }
      }
    });
    th.start();

    XYPlot plot = (XYPlot) closenessChartFrame.getChartPanel().getChart().getPlot();
    plot.setDataset(plot.getDatasetCount(),
                    ((XYPlot) demo.getChartPanel().getChart().getPlot()).getDataset(0));
  }

  private void runSimulation() {

    Thread th = new Thread(() -> {

      try {
        getDataFromOPC();
      } catch (InterruptedException e) {
        LOGGER.error("Error during data retrieving", e);
      }
    });
    th.start();
  }

  private void getDataFromOPC()
  throws InterruptedException {
//int k=0;
    while (true) {
      //if(k==0) {
        opcAccessApi.writeValueForTag(filter_p102, Boolean.TRUE);
      //  k=1;
     // }
      Thread.sleep(2000);
      if (filter_fake_risk_value > 0.3d)
      {
        opcAccessApi.writeValueForTag(filter_TP_1M7, Boolean.TRUE);}


      if (filter_fake_risk_value > 0.9d) {
        filter_fake_risk_value = 0.3d;
        filter_fake_active_flag = false;
        opcAccessApi.writeValueForTag(filter_p102, Boolean.FALSE);

        notifier(String.format("Вероятность аварии на станции фильтрации =%s " + "->\n" +
                    "Рекомендуемое действие=%s",
                "ВЫСОКАЯ", "Сброс давления в фильтре, отключение насосов", 0.1),
            filter_fake_risk_value) ;


        opcAccessApi.writeValueForTag(filter_TP_1M6, Boolean.TRUE);
        opcAccessApi.writeValueForTag(filter_TP_1M6, Boolean.FALSE);
        opcAccessApi.writeValueForTag(filter_p101, Boolean.TRUE);
        opcAccessApi.writeValueForTag(filter_TP_1M7, Boolean.FALSE);
        Thread.sleep(5000);
        opcAccessApi.writeValueForTag(filter_p101, Boolean.FALSE);
        opcAccessApi.writeValueForTag(filter_p102, Boolean.TRUE);
        Thread.sleep(10000);
        opcAccessApi.writeValueForTag(filter_p102, Boolean.FALSE);


return;
      }
//      tankOverheatClosenessValue = opcAccessApi.readFloat(reactorTemperatureSensor).value;
//      temperatureGrowthVal = opcAccessApi.readFloat(reactorTemperature).value;
//      decisionSupportList.getSeries().add(new Millisecond(), tankOverheatClosenessValue);
//      overflowRiskVal = opcAccessApi.readFloat(mixerRealSpeed).value;
//      if (tankOverheatClosenessValue >= MAX_TEMPERATURE) {
//        coolDownReactor();
//        notifier(String.format(
//            "Температура реактора=%s,\nВероятность перегрева=%s,\n" + "Рекомендуемое действие=%s",
//            tankOverheatClosenessValue,
//            ((tankOverheatClosenessValue - MAX_TEMPERATURE) / MAX_TEMPERATURE),
//            "Охлаждение реактора"), tankOverheatClosenessValue);
//      }
//      if (overflowRiskVal > MAX_FLOW_SPEED && opcAccessApi
//          .readBoolean(mixerToMaintTankPump).value) {
//        stopWaterFlowToMainTank();
//        notifier(String
//            .format("Вероятность переполнения бака=%s " + "->\n" + "Рекомендуемое действие=%s",
//                "ВЫСОКАЯ", "Остановка закачки воды, сброс излишков воды", 0.1), overflowRiskVal);
//      }
//
//      if (opcAccessApi.readFloat(mixerRealSpeed).value - opcAccessApi.readFloat(mixerSpeed).value
//          > 10.0f
//          && opcAccessApi.readBoolean(mixerToMaintTankPump).value) {
//        notifier(
//            String.format(
//                "Вероятность образования пузырьков в расходомере - ВЫСОКАЯ" + "->\n"
//                    + "Рекомендуемое"
//                    + " действие=%s",
//                "1. Проверка наличия воды в баках \r\n 2. Проверка, закрыты ли клапаны"),
//            overflowRiskVal);
//      }
    }
  }

  private void stopWaterFlowToMainTank()
  throws InterruptedException {
    opcAccessApi.writeValueForTag(mixerRealSpeed, 0f);
    opcAccessApi.writeValueForTag(mixerToMaintTankPump, false);
    opcAccessApi.writeValueForTag(downstream, true);
    Thread.sleep(1000);
    opcAccessApi.writeValueForTag(downstream, false);
  }

  private void coolDownReactor()
  throws InterruptedException {
    opcAccessApi.writeValueForTag(reactorTemperature, 0f);
    opcAccessApi.writeValueForTag(reactorHeater, false);
    opcAccessApi.writeValueForTag(mixerValve1, true);
    opcAccessApi.writeValueForTag(mixerValve2, true);
    opcAccessApi.writeValueForTag(mixerValve3, true);
    opcAccessApi.writeValueForTag(mixerRealSpeed, 40f);
    opcAccessApi.writeValueForTag(downstream, true);
    Thread.sleep(1000);
    opcAccessApi.writeValueForTag(downstream, false);
    opcAccessApi.writeValueForTag(reactorCooler, true);
  }

  private static void notifier(String term, double dangerLevel) {

    if (dangerLevel > 0.5d) {
      ImageIcon icon = new ImageIcon(EmergencyPredictionWindow.class.getResource("/alarm.png"));
      showErrorNotif(term, new Color(249, 78, 30), icon);
    }
    if (dangerLevel > 0.0d && dangerLevel <= 0.3d) {
      ImageIcon icon = new ImageIcon(EmergencyPredictionWindow.class.getResource("/info.png"));
      showErrorNotif(term, new Color(127, 176, 72), icon);
    }
    if (dangerLevel > 0.3d && dangerLevel <= 0.5d) {
      ImageIcon icon = new ImageIcon(EmergencyPredictionWindow.class.getResource("/warning.png"));
      showErrorNotif(term, new Color(249, 236, 100), icon);
    }
  }

  private static void showErrorNotif(String term, Color color, ImageIcon icon) {
    try {
      Thread.sleep(2000);
    } catch (InterruptedException ex) {
      Logger.getLogger(PopupTester.class.getName()).log(Level.SEVERE, null, ex);
    }
    notifications.add(term);
    if (decisionSupportList != null) {
      decisionSupportList.getList().setListData(notifications.toArray());
      decisionSupportList.getList().updateUI();
      decisionSupportList.getFrame().repaint();
    }
  /*  NotificationPopup nf = new NotificationPopup(term);
    nf.setIcon(icon);
    nf.setWIDTH(650);
    nf.setHEIGHT(100);
    nf.setLocation(10, 10);
    nf.setFont(new Font("Tachoma", Font.LAYOUT_LEFT_TO_RIGHT, 12));
    nf.setAlwaysOnTop(true);

    nf.setTitle("Ошибка");
    nf.setDisplayTime(3000);
    nf.setBackgroundColor1(Color.white);
    nf.setBackGroundColor2(color);
    nf.setForegroundColor(java.awt.Color.darkGray);
    nf.display();*/
    try {
      Thread.sleep(1000);
    } catch (InterruptedException ex) {
      Logger.getLogger(PopupTester.class.getName()).log(Level.SEVERE, null, ex);
    }
  }

  private static DynamicDataChart closenessChartFrame = new DynamicDataChart(
      "Вероятность возникновения аварийной ситуации для ст.фильт.");

  private boolean showUserDecisionDialog() {
    // show a joptionpane dialog using showMessageDialog
    Object[] options = {"Включить миксер", "Охладить реактор", "Охладить на 20",
        "Остановить нагрев"};
    int n = JOptionPane.showOptionDialog(closenessChartFrame, "Принятие решений?", "Вывод из НС",
        JOptionPane.YES_NO_CANCEL_OPTION, JOptionPane.QUESTION_MESSAGE, null, options, options[2]);
    if (n == 1) {
      opcAccessApi.writeValueForTag(downstream, true);
    }
    if (n == 2) {
    }
    if (n == 3) {
      opcAccessApi.writeValueForTag(reactorCooler, false);
      opcAccessApi.writeValueForTag(reactorHeater, false);
      return true;
    }
    if (n == 0) {
      opcAccessApi.writeValueForTag(reactorCooler, true);
    }
    return false;
  }

  private static final int MAX_FLOW_SPEED = 50;
}