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
package com.example.gateway.impl

import scala.collection.immutable.HashMap
import java.{ util => ju }
import collection.JavaConversions._
import com.example.gateway.PricingEngine
import util.Random
import util.Random.shuffle
import com.example.gateway.messages.QuoteRequest
import aQute.bnd.annotation.component.Reference
import com.example.gateway.messages.QuoteRequestReject

trait PricerSupport {
  private var pricers = new HashMap[String, List[PricingEngine]]
  private val lock = new AnyRef

  @Reference(`type` = '*')
  def addPricer(pricer: PricingEngine, attributes: ju.Map[String, Any]) {
    val t = attributes.getOrElse(PricingEngine.TYPE, PricingEngine.INDICATIVE_TYPE).asInstanceOf[String]

    lock.synchronized {
      val list = pricers.getOrElse(t, Nil);
      pricers += t -> (pricer :: list)
    }
  }

  def removePricer(pricer: PricingEngine, attributes: ju.Map[String, Any]) {
    val t = attributes.getOrElse(PricingEngine.TYPE, PricingEngine.INDICATIVE_TYPE).asInstanceOf[String]

    lock.synchronized {
      val list = pricers.getOrElse(t, Nil).filterNot(_ eq pricer);
      if (list.isEmpty) {
        pricers -= t
      } else {
        pricers += t -> list
      }
    }
  }

  protected def gatewayID: String

  def submitPrice(clientID: String, quote: QuoteRequest): Boolean = {
    val id = gatewayID

    // an alternative algorithm...
//    def findNextPricerRoundRobin(typeName: String): Option[PricingEngine] = {
//      lock.synchronized {
//        val list = pricers.getOrElse(typeName, List.empty[PricingEngine])
//
//        list.headOption match {
//          case s @ Some(pricer) => {
//            pricers += typeName -> (list.drop(1) ::: (pricer :: Nil))
//            s
//          }
//          case None => None
//        }
//      }
//    }
//
    def findNextPricerRandom(typeName: String): Option[PricingEngine] = {
      val list = lock.synchronized {
        pricers.getOrElse(typeName, Nil)
      }

      if (list.isEmpty) {
        None
      } else {
        list.drop(Random.nextInt(list.size) - 1).headOption
      }
    }

    def findNextPricer(typeName: String) = findNextPricerRandom(typeName);

    val typeName = if (quote.isIndicative) PricingEngine.INDICATIVE_TYPE else PricingEngine.FIRM_TYPE

    findNextPricer(typeName) match {
      case Some(pricer) => {
        pricer.price(id, clientID, quote)
        true
      }
      case None => false
    }
  }

}