package com.dronecontrol.perceptual.components;

import com.google.common.collect.Maps;
import com.google.common.collect.Sets;
import com.google.inject.Inject;
import com.dronecontrol.perceptual.PerceptualPipeline;
import com.dronecontrol.perceptual.components.filters.BilateralFilter;
import com.dronecontrol.perceptual.components.filters.Filter;
import com.dronecontrol.perceptual.data.DetectionType;
import com.dronecontrol.perceptual.data.body.BodyPart;
import com.dronecontrol.perceptual.data.body.Coordinate;
import com.dronecontrol.perceptual.data.body.Hand;
import com.dronecontrol.perceptual.data.events.DetectionData;
import com.dronecontrol.perceptual.data.events.HandsDetectionData;
import com.dronecontrol.perceptual.listeners.DetectionListener;
import intel.pcsdk.PXCMGesture;

import java.util.Map;
import java.util.Set;

public class DetectionComponent implements PerceptualQueryComponent {

    private final Filter leftHandFilter;

    private final Filter rightHandFilter;

    private Map<DetectionType<?>, Set<DetectionListener<?>>> detectionListeners;
    private PXCMGesture.GeoNode leftHandGeoNode;
    private PXCMGesture.GeoNode rightHandGeoNode;

    @Inject
    public DetectionComponent(BilateralFilter leftHandFilter, BilateralFilter rightHandFilter) {
        this.leftHandFilter = leftHandFilter;
        this.rightHandFilter = rightHandFilter;

        detectionListeners = Maps.newHashMap();
    }

    @Override
    public void queryFeatures(PerceptualPipeline pipeline) {
        leftHandGeoNode = new PXCMGesture.GeoNode();
        rightHandGeoNode = new PXCMGesture.GeoNode();

        pipeline.QueryGeoNode(PXCMGesture.GeoNode.LABEL_BODY_HAND_RIGHT, leftHandGeoNode);
        pipeline.QueryGeoNode(PXCMGesture.GeoNode.LABEL_BODY_HAND_LEFT, rightHandGeoNode);
    }

    @Override
    public void processFeatures() {
        Hand rightHand = new Hand(getSmoothedCoordinate(rightHandFilter, rightHandGeoNode), getCoordinate(rightHandGeoNode), isActive(rightHandGeoNode));
        Hand leftHand = new Hand(getSmoothedCoordinate(leftHandFilter, leftHandGeoNode), getCoordinate(leftHandGeoNode), isActive(leftHandGeoNode));

        //Sometimes Hands are mixed up. Switch them in case that x-position of right hand is greater than left hands x-position
        if (rightHand.isActive() && leftHand.isActive()) {
            if (rightHand.getCoordinate().getX() > leftHand.getCoordinate().getX()) {
                Hand temporaryRightHand = rightHand;
                rightHand = leftHand;
                leftHand = temporaryRightHand;
            }
        }

        invokeDetectionListeners(DetectionType.HANDS, new HandsDetectionData(leftHand, rightHand));
    }

    private Coordinate getSmoothedCoordinate(Filter filter, PXCMGesture.GeoNode handGeoNode) {
        return filter.getFilteredCoordinate(getCoordinate(handGeoNode));
    }

    private Coordinate getCoordinate(PXCMGesture.GeoNode handGeoNode) {
        if (handGeoNode.positionWorld != null) {
            return new Coordinate(handGeoNode.positionWorld.x, handGeoNode.positionWorld.z, handGeoNode.positionWorld.y);
        } else {
            return null;
        }
    }

    private boolean isActive(PXCMGesture.GeoNode handGeoNode) {
        return handGeoNode.positionWorld != null;
    }

    public <T extends BodyPart> void addDetectionListener(DetectionType<T> detectionType, DetectionListener<T> listener) {
        if (!detectionListeners.containsKey(detectionType)) {
            detectionListeners.put(detectionType, Sets.<DetectionListener<?>>newLinkedHashSet());
        }

        Set<DetectionListener<?>> listeners = detectionListeners.get(detectionType);

        if (!listeners.contains(listener)) {
            listeners.add(listener);
        }
    }

    public <T extends BodyPart> void removeDetectionListener(DetectionType<T> detectionType, DetectionListener<T> listener) {
        if (!detectionListeners.containsKey(detectionType)) {
            return;
        }

        Set<DetectionListener<?>> listeners = detectionListeners.get(detectionType);

        if (!listeners.contains(listener)) {
            listeners.remove(listener);
        }
    }

    @SuppressWarnings("unchecked")
    public <T extends BodyPart> void invokeDetectionListeners(DetectionType<T> detectionType, DetectionData<T> data) {
        if (!detectionListeners.containsKey(detectionType)) {
            return;
        }

        Set<DetectionListener<?>> listeners = detectionListeners.get(detectionType);

        for (DetectionListener<?> detectionListener : listeners) {
            DetectionListener<T> specificListener = (DetectionListener<T>) detectionListener;
            specificListener.onDetection(data);
        }
    }
}