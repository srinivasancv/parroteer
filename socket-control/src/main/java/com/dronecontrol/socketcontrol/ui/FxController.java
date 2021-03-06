package com.dronecontrol.socketcontrol.ui;

import com.dronecontrol.socketcontrol.input.data.MovementData;
import com.dronecontrol.socketcontrol.input.events.MovementDataListener;
import com.google.common.collect.Sets;
import com.dronecontrol.droneapi.data.NavData;
import com.dronecontrol.droneapi.listeners.NavDataListener;
import com.dronecontrol.droneapi.listeners.VideoDataListener;
import com.dronecontrol.socketcontrol.helpers.RaceTimer;
import com.dronecontrol.socketcontrol.ui.data.UIAction;
import com.dronecontrol.socketcontrol.ui.listeners.UIActionListener;
import javafx.application.Platform;
import javafx.embed.swing.SwingFXUtils;
import javafx.event.ActionEvent;
import javafx.event.EventHandler;
import javafx.fxml.FXML;
import javafx.scene.control.Label;
import javafx.scene.control.Slider;
import javafx.scene.image.ImageView;
import javafx.scene.image.WritableImage;
import javafx.scene.layout.VBox;
import javafx.scene.paint.Paint;

import java.awt.image.BufferedImage;
import java.util.Set;

public class FxController implements VideoDataListener, NavDataListener, EventHandler<ActionEvent>, MovementDataListener
{
  private final Set<UIActionListener> uiActionListeners;

  @FXML
  private ImageView imageView;

  @FXML
  private VBox vbox;

  @FXML
  private Label labelBattery;

  @FXML
  private Label labelTimer;

  @FXML
  private Slider slideRoll;

  @FXML
  private Slider slidePitch;

  @FXML
  private Slider slideYaw;

  private WritableImage image;

  private RaceTimer raceTimer;

  public FxController()
  {
    uiActionListeners = Sets.newHashSet();
  }

  public void addUIActionListener(UIActionListener uiActionlistener)
  {
    if (!uiActionListeners.contains(uiActionlistener))
    {
      uiActionListeners.add(uiActionlistener);
    }
  }

  public void removeUIActionListener(UIActionListener uiActionlistener)
  {
    if (uiActionListeners.contains(uiActionlistener))
    {
      uiActionListeners.remove(uiActionlistener);
    }
  }

  public void onApplicationClose()
  {
    emitUIAction(UIAction.CLOSE_APPLICATION);
  }

  @FXML
  protected void onButtonTakeOffAction(ActionEvent event)
  {
    emitUIAction(UIAction.TAKE_OFF);
  }

  @FXML
  public void onButtonLandAction(ActionEvent actionEvent)
  {
    emitUIAction(UIAction.LAND);
  }

  @FXML
  public void onButtonFlatTrimAction(ActionEvent actionEvent)
  {
    emitUIAction(UIAction.FLAT_TRIM);
  }

  @FXML
  public void onButtonEmergencyAction(ActionEvent actionEvent)
  {
    emitUIAction(UIAction.EMERGENCY);
  }

  @FXML
  public void onButtonSwitchCameraAction(ActionEvent actionEvent)
  {
    emitUIAction(UIAction.SWITCH_CAMERA);
  }

  @FXML
  public void onButtonLedAnimationAction(ActionEvent actionEvent)
  {
    emitUIAction(UIAction.PLAY_LED_ANIMATION);
  }

  @FXML
  public void onButtonFlightAnimationAction(ActionEvent actionEvent)
  {
    emitUIAction(UIAction.PLAY_FLIGHT_ANIMATION);
  }

  public void emitUIAction(UIAction action)
  {
    for (UIActionListener listener : uiActionListeners)
    {
      listener.onAction(action);
    }
  }

  private void runOnFxThread(Runnable runnable)
  {
    Platform.runLater(runnable);
  }

  @Override
  public void onNavData(final NavData navData)
  {
    runOnFxThread(new Runnable()
    {
      @Override
      public void run()
      {
        setBatteryLabel(navData);
      }
    });
  }

  public void setBatteryLabel(NavData navData)
  {
    String batteryLevelText = "Battery: " + navData.getBatteryLevel() + "%";
    labelBattery.setText(batteryLevelText);

    if (navData.getState().isBatteryTooLow())
    {
      labelBattery.setTextFill(Paint.valueOf("red"));
    } else
    {
      labelBattery.setTextFill(Paint.valueOf("white"));
    }
  }

  @Override
  public void onVideoData(final BufferedImage droneImage)
  {
    runOnFxThread(new Runnable()
    {
      @Override
      public void run()
      {
        imageView.setFitWidth(vbox.getWidth() - 20);
        imageView.setFitHeight(vbox.getHeight() - 100);

        image = SwingFXUtils.toFXImage(droneImage, image);
        if (imageView.getImage() != image)
        {
          imageView.setImage(image);
        }
      }
    });
  }

  // Timer event
  @Override
  public void handle(ActionEvent actionEvent)
  {
    if (raceTimer != null)
    {
      long totalMillis = raceTimer.getElapsedTime();
      long seconds = totalMillis / 1000;
      long millis = totalMillis % 1000;

      labelTimer.setText(String.format("Time: %d,%03d", seconds, millis));
    }
  }

  public void setRaceTimer(RaceTimer raceTimer)
  {
    this.raceTimer = raceTimer;
  }

  @Override
  public void onMovementData(final MovementData movementData)
  {
    runOnFxThread(new Runnable() {
      @Override
      public void run() {
        slideRoll.setValue(movementData.getRoll());
        slidePitch.setValue(movementData.getPitch());
        slideYaw.setValue(movementData.getYaw());
      }
    });
  }
}