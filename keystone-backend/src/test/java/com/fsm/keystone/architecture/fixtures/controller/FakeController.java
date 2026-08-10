package com.fsm.keystone.architecture.fixtures.controller;

/**
 * Minimal controller-side class used by the ArchUnit harness self-verification tests.
 * ViolatingService imports this class to produce a synthetic layer violation.
 * This is a test fixture — it is never deployed.
 */
public class FakeController {
    public void controllerOperation() { }
}
