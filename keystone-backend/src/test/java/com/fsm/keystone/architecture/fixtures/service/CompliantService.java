package com.fsm.keystone.architecture.fixtures.service;

/**
 * Synthetic service fixture with no controller-layer dependency.
 * Used by LayerRulesTest to verify that the layer-violation rule PASSES this class.
 * This is a test fixture — it is never deployed.
 */
public class CompliantService {

    public void compliantOperation() {
        // Service logic that does not reach into the controller layer
    }
}
