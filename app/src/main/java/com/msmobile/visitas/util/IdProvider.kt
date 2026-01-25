package com.msmobile.visitas.util

import java.util.UUID

class IdProvider {
    fun generateId(): UUID {
        return UUID.randomUUID()
    }
}