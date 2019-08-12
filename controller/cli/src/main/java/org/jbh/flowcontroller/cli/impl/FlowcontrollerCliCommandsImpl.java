/*
 * Copyright © 2017 jinbohao and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.jbh.flowcontroller.cli.impl;

import org.opendaylight.controller.md.sal.binding.api.DataBroker;
import org.jbh.flowcontroller.cli.api.FlowcontrollerCliCommands;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class FlowcontrollerCliCommandsImpl implements FlowcontrollerCliCommands {

    private static final Logger LOG = LoggerFactory.getLogger(FlowcontrollerCliCommandsImpl.class);
    private final DataBroker dataBroker;

    public FlowcontrollerCliCommandsImpl(final DataBroker db) {
        this.dataBroker = db;
        LOG.info("FlowcontrollerCliCommandImpl initialized");
    }

    @Override
    public Object testCommand(Object testArgument) {
        return "This is a test implementation of test-command";
    }
}
