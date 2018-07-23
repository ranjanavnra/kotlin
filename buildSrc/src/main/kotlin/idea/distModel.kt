package org.jetbrains.kotlin.buildUtils.idea

import java.io.File
import java.io.PrintWriter


class BuildVFile(
        val parent: BuildVFile?,
        val name: String,
        val file: File = File(parent!!.file, name)
) {
    val child = mutableMapOf<String, BuildVFile>()

    val contents = mutableSetOf<DistContentElement>()

    override fun toString(): String = name

    val hasContents: Boolean = file.exists() || contents.isNotEmpty()

    fun relativePath(path: String): BuildVFile {
        val pathComponents = path.split(File.separatorChar)
        return pathComponents.fold(this) { parent: BuildVFile, childName: String ->
            try {
                parent.getOrCreateChild(childName)
            } catch (t: Throwable) {
                throw Exception("Error while processing path `$path`, components: `$pathComponents`, element: `$childName`", t)
            }
        }
    }

    fun getOrCreateChild(name: String): BuildVFile = child.getOrPut(name) {
        BuildVFile(this, name)
    }

    fun addContents(contents: DistContentElement) {
        this.contents.add(contents)
    }

    fun printTree(p: PrintWriter, depth: String = "") {
        p.println("$depth ${file.path} ${if (file.exists()) "EXISTED" else ""}:")
        contents.forEach {
            p.println("$depth+ $it")
        }
        child.values.forEach {
            it.printTree(p, "$depth-")
        }
    }

}

sealed class DistContentElement(val targetDir: BuildVFile)

///////

class DistCopy(target: BuildVFile, val src: BuildVFile) : DistContentElement(target) {
    init {
        target.addContents(this)
    }

    override fun toString(): String = "COPY OF ${src.file}"
}

class DistModuleOutput(parent: BuildVFile, val projectId: String) : DistContentElement(parent) {
    init {
        parent.addContents(this)
    }

    override fun toString(): String = "COMPILE OUTPUT $projectId"

    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (javaClass != other?.javaClass) return false

        other as DistModuleOutput

        if (projectId != other.projectId) return false

        return true
    }

    override fun hashCode(): Int {
        return projectId.hashCode()
    }
}