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

package com.example.gateway.pricer

import java.lang.String
import java.{util => ju}
import com.example.gateway.{PricingEngineClient, PricingEngine}
import collection.JavaConversions._
import actors.Actor._
import com.example.gateway.messages.{Quote, QuoteRequest}
import java.util.Random
import collection.mutable.HashMap
import aQute.bnd.annotation.component.Component


@Component
class PricerImpl extends PricingEngine {
  private val clients = new HashMap[String, ActorPricingEngineClient]

  private implicit def pricingEngineClientToActorPricingEngineClient(client: PricingEngineClient) = new ActorPricingEngineClient(client)

  private var indicative = true;

  def activate(properties: ju.Map[String, Any]) {
    indicative = properties.toMap.get(PricingEngine.TYPE) match {
      case Some(str: String) => {
        if (str == PricingEngine.INDICATIVE_TYPE) true
        else if (str == PricingEngine.FIRM_TYPE) false
        else throw new IllegalArgumentException("Invalid pricing engine type " + str)
      }
      case Some(other) => throw new IllegalArgumentException("Invalid id type")
      case None => throw new IllegalStateException("No id specified")
    }
  }


  def addClient(client: PricingEngineClient, attributes: ju.Map[String, Any]) {
    attributes.toMap.get(PricingEngineClient.ID) match {
      case Some(id: String) => {
        clients.synchronized {
          clients += id -> client
        }
      }
      case Some(other) => println("Invalid id for " + client + "->" + other)
      case None => println("Missing id for " + client)
    }
  }

  def removeClient(client: PricingEngineClient, attributes: ju.Map[String, Any]) {
    attributes.toMap.get(PricingEngineClient.ID) match {
      case Some(id: String) => {
        clients.synchronized {
          clients -= id
        }
      }
      case Some(other) => // TODO log invalid attributes
      case None => // TODO log invalid attributes
    }
  }

  private val background = actor {
    val random = new Random
    val lastQuote = new HashMap[String, Int]
    loop {
      react{
        case (gatewayID: String, clientID: String, request: QuoteRequest) => {
          val last = lastQuote.synchronized {
            lastQuote.getOrElseUpdate(request.getUnderlying, random.nextInt(1000))
          }

          val price = last - 10 + random.nextInt(20)
          val quote = new Quote(request.getId, request.getUnderlying, price, indicative)

          clients.get(gatewayID) match {
            case Some(actor) => actor.background ! (clientID, quote)
            case None => System.out.println("No client found for " + gatewayID)
          }
        }
        case other => println("Discarding " + other)
      }
    }
  }

  def price(gatewayID: String, clientID: String, request: QuoteRequest) = {
    background ! (gatewayID, clientID, request)
  }
}

private class ActorPricingEngineClient(underlying: PricingEngineClient) {
  val background = actor {
    loop {
      receive {
        case (clientID: String, quote: Quote) => {
          try {
            underlying.receivePrice(clientID, quote)
          }
          catch {
            // TODO better logging
            case e: Throwable => e.printStackTrace()
          }
        }
      }
    }
  }
}