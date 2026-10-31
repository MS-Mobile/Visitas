package com.msmobile.visitas.di

import javax.inject.Qualifier

/** A scope that lives as long as the process, for work no screen should cancel. */
@Qualifier
@Retention(AnnotationRetention.BINARY)
annotation class ApplicationScope
