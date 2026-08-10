package com.fsm.keystone.architecture.fixtures.service;

import com.fsm.keystone.architecture.fixtures.controller.FakeController;

/**
 * Synthetic service fixture that deliberately imports a controller-layer class.
 * Used by LayerRulesTest to verify that the layer-violation rule DETECTS this class.
 * This is a test fixture — it is never deployed.
 */
public class ViolatingService {

    @SuppressWarnings("unused")
    private FakeController illegalControllerDependency;

    public void illegalOperation() {
        illegalControllerDependency = new FakeController();
        illegalControllerDependency.controllerOperation();
    }
}
