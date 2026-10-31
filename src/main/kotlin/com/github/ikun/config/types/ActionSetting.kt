package com.github.ikun.config.types

import com.github.ikun.config.ConfigHolder

class ActionSetting(name: String, val action: () -> Unit): ConfigHolder<Unit>(name, Unit)
