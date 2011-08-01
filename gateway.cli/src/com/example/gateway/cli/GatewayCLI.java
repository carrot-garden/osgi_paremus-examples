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
import com.example.gateway.GatewayClient;
import com.example.gateway.messages.Message;
import com.example.gateway.messages.QuoteRequest;
import org.apache.commons.cli.*;
import org.osgi.framework.BundleContext;
import org.osgi.framework.ServiceRegistration;
import org.osgi.util.tracker.ServiceTracker;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Hashtable;
import java.util.concurrent.atomic.AtomicLong;

public class GatewayCLI implements GatewayClient {

	static final String scope = "gateway";

	static final String[] functions = new String[] { "login", "request",
			"disconnect" };

	private Object loginToken;

	private static final Options options;

	static {
		options = new Options();

		Option indicativeOption = new Option("i", "indicative", false,
				"Request an indicative price");
		options.addOption(indicativeOption);
	}

	private final AtomicLong seq = new AtomicLong();
	private final BundleContext ctx;
	private final ServiceTracker gatewayTracker;
	private ServiceRegistration reg;

	public GatewayCLI(ServiceTracker gatewayTracker, BundleContext ctx) {
		this.gatewayTracker = gatewayTracker;
		this.ctx = ctx;
	}

	public void login(String name) {
		Hashtable<String, String> props = new Hashtable<String, String>();
		props.put(GatewayClient.ID, name);
		props.put("service.exported.interfaces", "*");

		if (reg == null) {
			reg = ctx.registerService(GatewayClient.class.getName(), this,
					props);
		} else {
			// need to unregister/reregister as remote services have no update
			// concept
			reg.unregister();
			reg = ctx.registerService(GatewayClient.class.getName(), this,
					props);
		}

		loginToken = gateway().connect(name);
	}

	public void request(String[] xargs) throws ParseException {
		if (loginToken == null) {
			System.err.println("Must call login first");
			return;
		}

		CommandLine foo = new GnuParser().parse(options, xargs);
		String[] args = foo.getArgs();

		boolean indicative = foo.hasOption("indicative");

		ArrayList<QuoteRequest> requests = new ArrayList<QuoteRequest>(
				args.length);

		for (String underlying : args) {
			requests.add(new QuoteRequest(seq.incrementAndGet(), underlying,
					indicative));
		}

		gateway().request(loginToken, requests);
	}

	public void disconnect() {
		if (loginToken != null) {
			gateway().disconnect(loginToken);
		}
	}

	private Gateway gateway() {
		Gateway gateway = (Gateway) gatewayTracker.getService();
		if (gateway == null)
			throw new IllegalStateException("Gateway service unavailable");
		return gateway;
	}

	public void receive(Collection<Message> messages) {
		for (Message m : messages) {
			System.out.println(m);
		}
	}
}
