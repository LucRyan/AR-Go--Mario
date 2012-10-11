package ryan.game.test;

import org.andengine.engine.camera.Camera;
import org.andengine.engine.options.EngineOptions;
import org.andengine.engine.options.EngineOptions.ScreenOrientation;
import org.andengine.engine.options.resolutionpolicy.RatioResolutionPolicy;
import org.andengine.entity.primitive.Rectangle;
import org.andengine.entity.scene.Scene;
import org.andengine.entity.scene.Scene.IOnSceneTouchListener;
import org.andengine.entity.scene.background.Background;
import org.andengine.entity.sprite.AnimatedSprite;
import org.andengine.entity.util.FPSLogger;
import org.andengine.extension.physics.box2d.PhysicsConnector;
import org.andengine.extension.physics.box2d.PhysicsFactory;
import org.andengine.extension.physics.box2d.PhysicsWorld;
import org.andengine.extension.physics.box2d.util.Vector2Pool;
import org.andengine.input.sensor.accelerometer.AccelerometerData;
import org.andengine.input.sensor.accelerometer.IAccelerometerListener;
import org.andengine.input.touch.TouchEvent;
import org.andengine.opengl.texture.TextureOptions;
import org.andengine.opengl.texture.atlas.bitmap.BitmapTextureAtlas;
import org.andengine.opengl.texture.atlas.bitmap.BitmapTextureAtlasTextureRegionFactory;
import org.andengine.opengl.texture.region.TiledTextureRegion;
import org.andengine.opengl.vbo.VertexBufferObjectManager;
import org.andengine.ui.activity.SimpleBaseGameActivity;
import android.hardware.SensorManager;

import com.badlogic.gdx.math.Vector2;
import com.badlogic.gdx.physics.box2d.Body;
import com.badlogic.gdx.physics.box2d.FixtureDef;
import com.badlogic.gdx.physics.box2d.BodyDef.BodyType;

public class MyGameActivity extends SimpleBaseGameActivity implements IAccelerometerListener, IOnSceneTouchListener {
	// ===========================================================
	// Constants
	// ===========================================================

	private static final int CAMERA_WIDTH = 480;
	private static final int CAMERA_HEIGHT = 320;

	// ===========================================================
	// Fields
	// ===========================================================

	private PhysicsWorld mPhysicsWorld;
	
	private BitmapTextureAtlas mBitmapTextureAtlas;

	private TiledTextureRegion mMarioTextureRegion;
	private TiledTextureRegion mMarioJumpTextureRegion;

	
	private Scene mScene;
	private float mGravityX;
	private float mGravityY;

	private AnimatedSprite mario;
	private AnimatedSprite marioRight;
	private AnimatedSprite marioLeft;
	private AnimatedSprite marioJump;
	private boolean marioMoveLeft = false;
	private boolean marioMoveRight = false;
	private boolean marioStand;
	private BitmapTextureAtlas mBitmapJumpTextureAtlas;




	// ===========================================================
	// Constructors
	// ===========================================================

	// ===========================================================
	// Getter & Setter
	// ===========================================================

	// ===========================================================
	// Methods for/from SuperClass/Interfaces
	// ===========================================================

	@Override
	public EngineOptions onCreateEngineOptions() {
		final Camera camera = new Camera(0, 0, CAMERA_WIDTH, CAMERA_HEIGHT);
		
		return new EngineOptions(true, ScreenOrientation.LANDSCAPE_FIXED, new RatioResolutionPolicy(CAMERA_WIDTH, CAMERA_HEIGHT), camera);
	}

	@Override
	public void onCreateResources() {
		//Set the texture base path
		BitmapTextureAtlasTextureRegionFactory.setAssetBasePath("sprites/");

		//Set the TextrueAtlas size
//		this.mBitmapTextureAtlas = new BitmapTextureAtlas(256, 256, TextureOptions.NEAREST);
		this.mBitmapTextureAtlas = new BitmapTextureAtlas(256, 256, TextureOptions.BILINEAR);
		this.mBitmapJumpTextureAtlas = new BitmapTextureAtlas(256, 256, TextureOptions.BILINEAR);
		
		//Set the region and specific sprite
		this.mMarioTextureRegion = BitmapTextureAtlasTextureRegionFactory.createTiledFromAsset(this.mBitmapTextureAtlas, this, "mario_walk.png", 0, 0, 3, 2);
		this.mMarioJumpTextureRegion = BitmapTextureAtlasTextureRegionFactory.createTiledFromAsset(this.mBitmapJumpTextureAtlas, this, "mario_jump.png", 0, 0, 1, 2);
		this.mEngine.getTextureManager().loadTexture(mBitmapTextureAtlas);
	}

	@Override
	public Scene onCreateScene() {
		//Set FPSlogger and physics World
		this.mEngine.registerUpdateHandler(new FPSLogger());
		this.mPhysicsWorld = new PhysicsWorld(new Vector2(0, SensorManager.GRAVITY_EARTH), false);
		
		//Create new Scene
		this.mScene = new Scene();
		mScene.setBackground(new Background(0,0,0));
		
		//Initialize the physical boundary
		final VertexBufferObjectManager vertexBufferObjectManager = this.getVertexBufferObjectManager();
		final Rectangle ground = new Rectangle(0, CAMERA_HEIGHT - 2, CAMERA_WIDTH, 2, vertexBufferObjectManager);
		final Rectangle roof = new Rectangle(0, 0, CAMERA_WIDTH, 2, vertexBufferObjectManager);
		final Rectangle left = new Rectangle(0, 0, 2, CAMERA_HEIGHT, vertexBufferObjectManager);
		final Rectangle right = new Rectangle(CAMERA_WIDTH - 2, 0, 2, CAMERA_HEIGHT, vertexBufferObjectManager);

		//Create the physical body of boundary
		final FixtureDef wallFixtureDef = PhysicsFactory.createFixtureDef(0, 0.5f, 0.5f);
		PhysicsFactory.createBoxBody(this.mPhysicsWorld, ground, BodyType.StaticBody, wallFixtureDef);
		PhysicsFactory.createBoxBody(this.mPhysicsWorld, roof, BodyType.StaticBody, wallFixtureDef);
		PhysicsFactory.createBoxBody(this.mPhysicsWorld, left, BodyType.StaticBody, wallFixtureDef);
		PhysicsFactory.createBoxBody(this.mPhysicsWorld, right, BodyType.StaticBody, wallFixtureDef);

		//Add the bodies to the scene
		this.mScene.attachChild(ground);
		this.mScene.attachChild(roof);
		this.mScene.attachChild(left);
		this.mScene.attachChild(right);
		addMario(50,50);
		
		//
		this.mScene.registerUpdateHandler(this.mPhysicsWorld);
		this.mScene.setOnSceneTouchListener(this);
		
		return mScene;
	}
	
	@Override
	public boolean onSceneTouchEvent(final Scene pScene, final TouchEvent pSceneTouchEvent) {
		if(this.mPhysicsWorld != null) {
			//When player touch the screen
			if(pSceneTouchEvent.isActionDown()) {
				this.jumpMario(mario);
				return true;
			}
		}
		return false;
	}


	@Override
	public void onAccelerometerChanged(final AccelerometerData pAccelerometerData) {
		this.mGravityX = pAccelerometerData.getX();
		this.mGravityY = pAccelerometerData.getY();
		
		if(pAccelerometerData.getX() > 0){
			marioMoveRight = true;	
			marioMoveLeft = false;
			marioStand = false;
		}else if (pAccelerometerData.getX() == 0){
			marioMoveLeft = false;
			marioMoveRight = false;
			marioStand = true;
		}else if (pAccelerometerData.getX() < 0){
			marioStand = false;
			marioMoveLeft = true;
			marioMoveRight = false;
		}
		
		float bufX = mario.getX();
		float bufY = mario.getY();
		
		if(marioMoveLeft && mario != marioLeft){
			mario.setVisible(false);
			mario = marioLeft;
		}else if(marioMoveRight && mario != marioRight){
			mario.setVisible(false);
			mario = marioRight;
		}else if(marioStand){
			mario.setVisible(false);
		}
		mario.setPosition(bufX, bufY);
		mario.setVisible(true);
		
		final Vector2 gravity = Vector2Pool.obtain(this.mGravityX * 5 , this.mGravityY);
		this.mPhysicsWorld.setGravity(gravity);
		Vector2Pool.recycle(gravity);
	}

	@Override
	public void onResumeGame() {
		super.onResumeGame();

		this.enableAccelerometerSensor(this);
	}

	@Override
	public void onPauseGame() {
		super.onPauseGame();

		this.disableAccelerometerSensor();
	}

	// ===========================================================
	// Methods
	// ===========================================================
	private void addMario(final float pX, final float pY) {
		final Body body;

		final FixtureDef objectFixtureDef = PhysicsFactory.createFixtureDef(1, 0.5f, 0.5f);

		mario = new AnimatedSprite(pX, pY, this.mMarioTextureRegion, this.getVertexBufferObjectManager());
		mario.animate(new long[] { 100, 100, 100 }, 3, 5, true);
		body = PhysicsFactory.createBoxBody(this.mPhysicsWorld, mario, BodyType.DynamicBody, objectFixtureDef);
		
		marioRight = new AnimatedSprite(pX, pY, this.mMarioTextureRegion, this.getVertexBufferObjectManager());
		marioRight.animate(new long[] { 100, 100, 100 }, 3, 5, true);
		//bodyRight = PhysicsFactory.createBoxBody(this.mPhysicsWorld, marioRight, BodyType.DynamicBody, objectFixtureDef);
		
		marioLeft = new AnimatedSprite(pX, pY, this.mMarioTextureRegion, this.getVertexBufferObjectManager());
		marioLeft.animate(new long[] { 100, 100, 100 }, 0, 2, true);
		//bodyLeft = PhysicsFactory.createBoxBody(this.mPhysicsWorld, marioLeft, BodyType.DynamicBody, objectFixtureDef);
		
		//marioJump = new AnimatedSprite(pX, pY, this.mMarioJumpTextureRegion, this.getVertexBufferObjectManager());
		//marioJump.animate(new long[] { 100, 1000 }, 0, 1, true);
		
		this.mPhysicsWorld.registerPhysicsConnector(new PhysicsConnector(mario, body, true, true));
		this.mPhysicsWorld.registerPhysicsConnector(new PhysicsConnector(marioRight, body, true, true));
		this.mPhysicsWorld.registerPhysicsConnector(new PhysicsConnector(marioLeft, body, true, true));
		//this.mPhysicsWorld.registerPhysicsConnector(new PhysicsConnector(marioJump, body, true, true));
		
		mario.setUserData(body);
		this.mScene.attachChild(mario);
		
		marioRight.setUserData(body);
		this.mScene.attachChild(marioRight);
		
		marioLeft.setUserData(body);
		this.mScene.attachChild(marioLeft);
		
		//marioJump.setUserData(body);
		//this.mScene.attachChild(marioJump);
		
		marioRight.setVisible(false);
		marioLeft.setVisible(false);
		//marioJump.setVisible(false);
	}

	private void jumpMario(final AnimatedSprite face) {
		final Body faceBody = (Body)face.getUserData();
		
		//float bufX = mario.getX();
		//float bufY = mario.getY();
		//mario.setVisible(false);
		//mario = marioJump;
		//mario.setPosition(bufX, bufY);
		//mario.setVisible(true);
		
		final Vector2 velocity = Vector2Pool.obtain(this.mGravityX * -1, (float) (this.mGravityY * -0.7));
		faceBody.setLinearVelocity(velocity);
		Vector2Pool.recycle(velocity);
	}
	// ===========================================================
	// Inner and Anonymous Classes
	// ===========================================================
}
