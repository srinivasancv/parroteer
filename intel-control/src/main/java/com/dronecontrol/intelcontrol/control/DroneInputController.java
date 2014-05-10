package com.dronecontrol.intelcontrol.control;

import com.dronecontrol.droneapi.DroneController;
import com.dronecontrol.droneapi.data.NavData;
import com.dronecontrol.droneapi.listeners.NavDataListener;
import com.dronecontrol.droneapi.listeners.ReadyStateChangeListener;
import com.dronecontrol.intelcontrol.helpers.RaceTimer;
import com.dronecontrol.intelcontrol.ui.data.UIAction;
import com.dronecontrol.intelcontrol.ui.listeners.UIActionListener;
import com.dronecontrol.perceptual.data.body.Hand;
import com.dronecontrol.perceptual.data.body.Hands;
import com.dronecontrol.perceptual.data.events.DetectionData;
import com.dronecontrol.perceptual.data.events.GestureData;
import com.dronecontrol.perceptual.data.events.HandsDetectionData;
import com.dronecontrol.perceptual.helpers.CoordinateCalculator;
import com.dronecontrol.perceptual.helpers.CoordinateListener;
import com.dronecontrol.perceptual.listeners.DetectionListener;
import com.dronecontrol.perceptual.listeners.GestureListener;
import com.google.inject.Inject;
import org.apache.log4j.Logger;

public class DroneInputController implements ReadyStateChangeListener, NavDataListener, UIActionListener,
        DetectionListener<Hands>, GestureListener, CoordinateListener {

    private static final float HEIGHT_THRESHOLD = 0.25f;

    private final Logger logger = Logger.getLogger(DroneInputController.class);

    private final DroneController droneController;

    private final RaceTimer raceTimer;

    private boolean flying = false;

    private boolean ready = false;

    private long lastCommandTimestamp;

    private CoordinateCalculator coordinateCalculator;

    @Inject
    public DroneInputController(DroneController droneController, RaceTimer raceTimer, CoordinateCalculator coordinateCalculator) {
        this.droneController = droneController;
        this.raceTimer = raceTimer;
        this.coordinateCalculator = coordinateCalculator;
        coordinateCalculator.addCoordinateListener(this);

        ready = true;
    }

    @Override
    public void onAction(UIAction action) {
        switch (action) {
            case TAKE_OFF:
                takeOff();
                break;
            case LAND:
                land();
                break;
            case FLAT_TRIM:
                flatTrim();
                break;
            case EMERGENCY:
                emergency();
                break;
            case SWITCH_CAMERA:
                switchCamera();
                break;
            case PLAY_LED_ANIMATION:
                playLedAnimation();
                break;
            case PLAY_FLIGHT_ANIMATION:
                playFlightAnimation();
                break;
        }
    }

    @Override
    public void onNavData(NavData navData) {
        flying = navData.getState().isFlying();
        coordinateCalculator.setCurrentHeight(navData.getAltitude() < HEIGHT_THRESHOLD ? 0.0f : navData.getAltitude());

        if (navData.getState().isEmergency()) {
            raceTimer.stop();
        }
    }

    private void takeOff() {
        if (ready && !flying) {
            raceTimer.start();
            //droneController.takeOff();
            flying = true;
        } else {
            logger.warn("Cannot take off");
        }
    }

    private void land() {
        if (ready && flying) {
            raceTimer.stop();
            //droneController.land();
            flying = false;
        }
    }

    private void flatTrim() {
        if (ready) {
        //    droneController.flatTrim();
        }
    }

    private void emergency() {
        if (ready) {
        //    droneController.emergency();
        }
    }

    private void switchCamera() {
        if (ready) {
        //    droneController.switchCamera(Camera.NEXT);
        }
    }

    private void playLedAnimation() {
        if (ready) {
        //    droneController.playLedAnimation(LedAnimation.RED_SNAKE, 2.0f, 3);
        }
    }

    private void playFlightAnimation() {
        if (ready) {
        //    droneController.playFlightAnimation(FlightAnimation.FLIP_LEFT);
        }
    }

    private void move(float roll, float pitch, float yaw, float heightDelta) {
        if (ready) {
        //    droneController.move(2 * roll, 2 * pitch, 2 * yaw, heightDelta);
        }
    }

    @Override
    public void onReadyStateChange(ReadyState readyState) {
        if (readyState == ReadyState.READY) {
            ready = true;
        } else if (readyState == ReadyState.NOT_READY) {
            ready = false;
        }
    }

    @Override
    public void onGesture(GestureData gestureData) {
        switch (gestureData.getGestureType()) {
            case CIRCLE:
                logger.info("Circle detected.");
                takeOff();
                break;
        }
    }

    @Override
    public void onDetection(DetectionData<Hands> handsDetectionData) {
        HandsDetectionData data = (HandsDetectionData) handsDetectionData;
        if (data.getLeftHand().isActive() || data.getRightHand().isActive()) {
            Hand firstHand = data.getLeftHand().isActive() ? data.getLeftHand() : data.getRightHand();
            lastCommandTimestamp = System.currentTimeMillis();

            if (ready && flying) {
                landIfHandIsTooCloseToCamera(firstHand);
                coordinateCalculator.calculateMoves(firstHand);
            }
        } else if ((System.currentTimeMillis() - lastCommandTimestamp) >= 50) {
            // Failsafe - If no information about hands is available don't move
            coordinateCalculator.resetMove();
        }
    }

    private void landIfHandIsTooCloseToCamera(Hand hand) {
        if (ready && flying && hand.getCoordinate().getZ() < HEIGHT_THRESHOLD) {
            land();
            coordinateCalculator.resetMove();
        }
    }

    @Override
    public void onCoordinate(float roll, float pitch, float yaw, float heightDelta) {
        //When changing the yaw stop rolling and pitching
        move(coordinateCalculator.getRoll(), coordinateCalculator.getPitch(), 0, coordinateCalculator.getHeightDelta());
    }
}