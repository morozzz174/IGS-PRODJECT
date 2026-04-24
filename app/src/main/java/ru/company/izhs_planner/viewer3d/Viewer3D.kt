package ru.company.izhs_planner.viewer3d

import android.content.Context
import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.Color
import android.opengl.GLSurfaceView
import ru.company.izhs_planner.domain.model.House
import ru.company.izhs_planner.domain.model.Room
import ru.company.izhs_planner.domain.model.Project
import java.nio.ByteBuffer
import java.nio.ByteOrder
import java.nio.FloatBuffer

class Viewer3D(private val context: Context) {
    private var surfaceView: GLSurfaceView? = null
    private var renderer: HouseRenderer? = null
    
    fun createSurface(context: Context): GLSurfaceView {
        val view = GLSurfaceView(context)
        renderer = HouseRenderer()
        view.setRenderer(renderer)
        view.renderMode = GLSurfaceView.RENDERMODE_WHEN_DIRTY
        surfaceView = view
        return view
    }
    
    fun loadProject(project: Project) {
        renderer?.loadProject(project)
        surfaceView?.requestRender()
    }
    
    fun rotate(angleX: Float, angleY: Float) {
        renderer?.rotate(angleX, angleY)
        surfaceView?.requestRender()
    }
    
    fun zoom(scale: Float) {
        renderer?.zoom(scale)
        surfaceView?.requestRender()
    }
    
    fun capture(width: Int, height: Int): Bitmap? {
        return renderer?.capture(width, height)
    }
    
    fun release() {
        surfaceView?.onPause()
        surfaceView = null
        renderer = null
    }
}

class HouseRenderer : android.opengl.GLSurfaceView.Renderer {
    private var vertices: FloatBuffer? = null
    private var colors: FloatBuffer? = null
    private var angleX = 30f
    private var angleY = 0f
    private var scale = 1f
    private var project: Project? = null
    
    fun loadProject(project: Project) {
        this.project = project
        generateMesh(project)
    }
    
    fun rotate(angleX: Float, angleY: Float) {
        this.angleX = (this.angleX + angleX).coerceIn(0f, 90f)
        this.angleY += angleY
    }
    
    fun zoom(scale: Float) {
        this.scale = (this.scale * scale).coerceIn(0.5f, 3f)
    }
    
    fun capture(width: Int, height: Int): Bitmap? {
        return null
    }
    
    private fun generateMesh(project: Project) {
        val house = project.house
        val roomList = project.floors.flatMap { it.rooms }
        
        val vertexList = mutableListOf<Float>()
        val colorList = mutableListOf<Float>()
        
        val baseOffset = 0.05f
        
        addBox(
            vertexList, colorList,
            0f, 0f, 0f,
            house.width, house.floorHeight * house.floors, house.length,
            0.6f, 0.75f, 0.6f
        )
        
        for ((index, room) in roomList.withIndex()) {
            val floorOffset = (room.floorNumber - 1) * house.floorHeight
            
            val colorShift = index * 0.05f
            val r = 0.3f + colorShift
            val g = 0.5f + (index % 3) * 0.15f
            val b = 0.7f
            
            addBox(
                vertexList, colorList,
                room.x + baseOffset, floorOffset + baseOffset, room.y + baseOffset,
                room.width - baseOffset * 2, house.floorHeight - baseOffset * 2, room.length - baseOffset * 2,
                r, g, b
            )
        }
        
        addGround(
            vertexList, colorList,
            house.width.toFloat() * 1.5f, house.length.toFloat() * 1.5f,
            -house.floorHeight * house.floors * 0.5f
        )
        
        vertices = ByteBuffer.allocateDirect(vertexList.size * 4)
            .order(ByteOrder.nativeOrder())
            .asFloatBuffer()
            .put(vertexList.toFloatArray())
        vertices?.position(0)
        
        colors = ByteBuffer.allocateDirect(colorList.size * 4)
            .order(ByteOrder.nativeOrder())
            .asFloatBuffer()
            .put(colorList.toFloatArray())
        colors?.position(0)
    }
    
    private fun addBox(
        vertexList: MutableList<Float>,
        colorList: MutableList<Float>,
        x: Float, y: Float, z: Float,
        width: Float, height: Float, length: Float,
        r: Float, g: Float, b: Float
    ) {
        val v = listOf(
            // Передняя грань
            x, y, z + length,
            x + width, y, z + length,
            x + width, y + height, z + length,
            x, y, z + length,
            x + width, y + height, z + length,
            x, y + height, z + length,
            
            // Задняя грань
            x + width, y, z,
            x, y, z,
            x, y + height, z,
            x + width, y, z,
            x, y + height, z,
            x + width, y + height, z,
            
            // Левая грань
            x, y, z,
            x, y, z + length,
            x, y + height, z + length,
            x, y, z,
            x, y + height, z + length,
            x, y + height, z,
            
            // Правая грань
            x + width, y, z + length,
            x + width, y, z,
            x + width, y + height, z,
            x + width, y, z + length,
            x + width, y + height, z,
            x + width, y + height, z + length,
            
            // Верхняя грань
            x, y + height, z + length,
            x + width, y + height, z + length,
            x + width, y + height, z,
            x, y + height, z + length,
            x + width, y + height, z,
            x, y + height, z,
            
            // Нижняя грань (пол)
            x, y, z,
            x + width, y, z,
            x + width, y, z + length,
            x, y, z,
            x + width, y, z + length,
            x, y, z + length
        )
        
        vertexList.addAll(v)
        
        for (i in 0..5) {
            colorList.addAll(listOf(r, g, b, 1f))
        }
    }
    
    private fun addGround(
        vertexList: MutableList<Float>,
        colorList: MutableList<Float>,
        width: Float, length: Float, y: Float
    ) {
        val v = listOf(
            0f, y, 0f,
            width, y, 0f,
            width, y, length,
            0f, y, 0f,
            width, y, length,
            0f, y, length
        )
        
        vertexList.addAll(v)
        
        for (i in 0..5) {
            colorList.addAll(listOf(0.4f, 0.6f, 0.3f, 1f))
        }
    }
    
    override fun onSurfaceCreated(gl: android.opengl.GLSurfaceView?, config: android.opengl.EGLConfig?) {
        android.opengl.GLES20.glClearColor(0.9f, 0.95f, 0.9f, 1f)
        android.opengl.GLES20.glEnable(android.opengl.GLES20.GL_DEPTH_TEST)
        android.opengl.GLES20.glEnable(android.opengl.GLES20.GL_COLOR_MATERIAL)
    }
    
    override fun onSurfaceChanged(gl: android.opengl.GLSurfaceView?, width: Int, height: Int) {
        android.opengl.GLES20.glViewport(0, 0, width, height)
    }
    
    override fun onDrawFrame(gl: android.opengl.GLSurfaceView?) {
        android.opengl.GLES20.glClear(android.opengl.GLES20.GL_COLOR_BUFFER_BIT or android.opengl.GLES20.GL_DEPTH_BUFFER_BIT)
        
        val vertexBuffer = vertices ?: return
        
        android.opengl.GLES20.glVertexAttribPointer(
            0, 3, android.opengl.GLES20.GL_FLOAT, false, 0, vertexBuffer
        )
        android.opengl.GLES20.glEnableVertexAttribArray(0)
        
        android.opengl.GLES20.glVertexAttribPointer(
            1, 4, android.opengl.GLES20.GL_FLOAT, false, 0, colors!!
        )
        android.opengl.GLES20.glEnableVertexAttribArray(1)
        
        android.opengl.GLES20.glDrawArrays(android.opengl.GLES20.GL_TRIANGLES, 0, vertexBuffer.capacity() / 3)
        
        android.opengl.GLES20.glDisableVertexAttribArray(0)
        android.opengl.GLES20.glDisableVertexAttribArray(1)
    }
}