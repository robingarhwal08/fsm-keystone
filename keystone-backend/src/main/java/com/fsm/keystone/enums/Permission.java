package com.fsm.keystone.enums;

import lombok.AllArgsConstructor;
import lombok.Getter;

@AllArgsConstructor
@Getter
public enum Permission {

    CUSTOMER_CREATE,
    CUSTOMER_UPDATE,
    CUSTOMER_DELETE,

    SITE_CREATE,
    SITE_UPDATE,
    SITE_DELETE,

    WORKORDER_CREATE,
    WORKORDER_ASSIGN,
    WORKORDER_COMPLETE,

    INVENTORY_CREATE,
    INVENTORY_UPDATE,

    USER_MANAGE,

    REPORT_VIEW
}