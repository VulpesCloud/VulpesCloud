package io.github.thecguygithub.node.terminal.setup.setups

import io.github.thecguygithub.node.terminal.setup.Setup

class TaskSetup() : Setup("Task-Setup") {

    init {

        question("name", "What is the Name of the Task you want to create?", s -> )

        question(
            "name",
            "What is the name of the group&8?",
            { s -> !Node.instance().groupProvider().exists(s.first()) })


    }

}