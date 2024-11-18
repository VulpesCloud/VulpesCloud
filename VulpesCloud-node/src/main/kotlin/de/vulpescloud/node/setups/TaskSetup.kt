package de.vulpescloud.node.setups

import de.vulpescloud.node.Node
import de.vulpescloud.node.setup.Setup
import de.vulpescloud.node.setup.annotations.SetupQuestion

class TaskSetup : Setup {

    private lateinit var name: String

    @SetupQuestion(0, "node.setup.task.question.name")
    fun name(name: String): Boolean {
        if (name.length > 16) {
            Node.terminal!!.printSetup(Node.languageProvider.translate("node.setup.task.question.name.long"))
            return false
        }
        if (name.isEmpty()) {
            Node.terminal!!.printSetup(Node.languageProvider.translate("node.setup.task.question.name.empty"))
            return false
        }
        this.name = name
        Node.terminal!!.printSetup(Node.languageProvider.translate("node.setup.task.question.name.success"))
        return true
    }
}