/*
 * Copyright © 2017 jinbohao and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.jbh.controller.cli.impl;

import org.opendaylight.controller.md.sal.binding.api.DataBroker;
import org.jbh.controller.cli.api.ControllerCliCommands;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class ControllerCliCommandsImpl implements ControllerCliCommands {

    private static final Logger LOG = LoggerFactory.getLogger(ControllerCliCommandsImpl.class);
    private final DataBroker dataBroker;

    public ControllerCliCommandsImpl(final DataBroker db) {
        this.dataBroker = db;
        LOG.info("ControllerCliCommandImpl initialized");
    }

    @Override
    public Object testCommand(Object testArgument) {
        return "This is a test implementation of test-command";
    }
}
