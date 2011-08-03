package com.example.gateway.pricer

import aQute.bnd.annotation.metatype.Meta.OCD

trait PricerConfig {
	def `type`: String // TODO this should be an enum but needs support from bnd to understand scala enums
}