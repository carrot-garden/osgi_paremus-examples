/* Copyright 2011 Paremus Limited
 * 
 * Licensed under the Apache License, Version 2.0 (the License)
 * you may not use this file except in compliance
 * with the License.  You may obtain a copy of the License at
 *
 *   http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing,
 * software distributed under the License is distributed on an
 * "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
 * KIND, either express or implied.  See the License for the
 * specific language governing permissions and limitations
 * under the License. 
 * see http://www.apache.org/licenses/LICENSE-2.0 
 */

package com.example.gateway.cli;

import com.example.gateway.Gateway;
import org.osgi.framework.BundleActivator;
import org.osgi.framework.BundleContext;
import org.osgi.framework.ServiceRegistration;
import org.osgi.util.tracker.ServiceTracker;

import java.util.Hashtable;

public class GatewayCLIActivator implements BundleActivator {

    private BundleContext ctx;
    private ServiceTracker gatewayTracker;
    private ServiceRegistration testReg;
    private ServiceRegistration cliReg;

    public void start(BundleContext bundleContext) throws Exception {
        this.ctx = bundleContext;

        gatewayTracker = new ServiceTracker(ctx, Gateway.class.getName(), null);
        gatewayTracker.open();

        Hashtable<String, Object> props = new Hashtable<String, Object>();

        props.put("osgi.command.scope", GatewayCLI.scope);
        props.put("osgi.command.function", GatewayCLI.functions);

        cliReg = ctx.registerService(GatewayCLI.class.getName(), new GatewayCLI(gatewayTracker, ctx), props);

        props.put("osgi.command.scope", GatewayTestCLI.scope);
        props.put("osgi.command.function", GatewayTestCLI.functions);

        testReg = ctx.registerService(GatewayTestCLI.class.getName(), new GatewayTestCLI(gatewayTracker, ctx), props);
    }

    public void stop(BundleContext bundleContext) throws Exception {
        if(cliReg != null) {
            cliReg.unregister();
            cliReg = null;
        }

        if(testReg != null) {
            testReg.unregister();
            testReg = null;
        }

        gatewayTracker.close();
        gatewayTracker = null;
        ctx = null;
    }
}
