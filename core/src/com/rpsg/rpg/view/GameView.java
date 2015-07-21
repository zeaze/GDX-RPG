package com.rpsg.rpg.view;

import box2dLight.RayHandler;

import com.badlogic.gdx.assets.AssetLoaderParameters;
import com.badlogic.gdx.assets.AssetManager;
import com.badlogic.gdx.graphics.OrthographicCamera;
import com.badlogic.gdx.graphics.g2d.SpriteBatch;
import com.badlogic.gdx.maps.tiled.*;
import com.badlogic.gdx.maps.tiled.renderers.OrthoCachedTiledMapRenderer;
import com.badlogic.gdx.physics.box2d.World;
import com.badlogic.gdx.scenes.scene2d.*;
import com.bitfire.postprocessing.PostProcessor;
import com.bitfire.postprocessing.effects.Bloom;
import com.rpsg.rpg.core.Setting;
import com.rpsg.rpg.object.base.Global;
import com.rpsg.rpg.object.base.IOMode;
import com.rpsg.rpg.object.rpg.Hero;
import com.rpsg.rpg.system.base.*;
import com.rpsg.rpg.system.controller.*;
import com.rpsg.rpg.system.ui.View;
import com.rpsg.rpg.system.ui.StackView;
import com.rpsg.rpg.utils.display.*;
import com.rpsg.rpg.utils.game.Logger;

public class GameView extends View{
	
	public OrthoCachedTiledMapRenderer render ;//地图绘制器
	public Stage stage = GameViewRes.stage;//舞台
	public TiledMap map;//地图文件
	public boolean inited=false;//是否加载完成的状态标志
	public Global global=GameViews.global;//游戏存档
	public OrthographicCamera camera = GameViewRes.camera;//摄像机
	public RayHandler ray = GameViewRes.ray;//灯光
	public World world =GameViewRes.world;//box2d world（没用）
	public StackView stackView;//菜单视图
	AssetManager ma=GameViewRes.ma;//资源管理器
	String filename;//地图文件名（卸载地图纹理时候用到）
	TmxMapLoader.Parameters parameter;//地图加载完成的回调
	
	public PostProcessor post;//高清画质用
	public Bloom bloom;//模糊用


	@Override
	public void init() {
		inited=false;
		Logger.info("开始加载图形。");
		stage.clear();
		if(PostUtil.first)
			PostUtil.init();
		parameter = new TmxMapLoader.Parameters();
		parameter.loadedCallback= new AssetLoaderParameters.LoadedCallback() {
			public void finishedLoading(AssetManager assetManager, String fileName, Class type) {
				map = ma.get(Setting.GAME_RES_MAP + global.map);
				render=new OrthoCachedTiledMapRenderer(map);
				render.setBlending(true);
				render.setView(camera);
				ray.setWorld(world);
				Initialization.init(GameView.this);
				inited = true;
				post = GameViews.post;
				bloom = GameViews.bloom;
				WeatherUtil.init(GameViews.global.weather);
				Logger.info("图形加载完成。");
			}
		};
		filename=Setting.GAME_RES_MAP+global.map;
		ma.load(filename, TiledMap.class ,parameter);
	}
	
	@Override
	public void dispose() {
		MapController.dispose();
		Msg.dispose();
		if(!Setting.persistence.cacheResource){
			map.dispose();
			ma.unload(filename);
		}
		if(null!=stackView){
			stackView.dispose();
			stackView=null;
			InputController.currentIOMode=IOMode.MapInput.NORMAL;
		}
		GameViewRes.ray.removeAll();
		parameter.loadedCallback=null;
		parameter=null;


		System.gc();
	}
	
	@Override
	public void draw(SpriteBatch batch) {
		if(!ma.update() || !inited){
			PostUtil.draw(false);
			return;
		}
		boolean postEnable=Setting.persistence.betterLight;
		
		if(postEnable)
			post.capture();
		
//		DistantController.draw((SpriteBatch)stage.getBatch(),this); XXX 远景
		
		MapController.draw(this);


		if(postEnable)
			post.render(true);
		
		if(Setting.persistence.betterLight && map.getProperties().get("weather")==null)
			WeatherUtil.draw((SpriteBatch) PostUtil.stage.getBatch());

		ColorUtil.draw();
		PostUtil.draw(true);

		DrawController.draw();
		
		if(null!=stackView)
			stackView.draw(batch);
		else
			ThreadPool.logic();

	}

	@Override
	public void logic() {
		if(!ma.update() || !inited)
			return;
		if(null==stackView){
			MapController.logic(this);
			for(Actor i:stage.getActors())
				if(!(i instanceof Hero))
					i.act(0);
			HeroController.act();
			MoveController.logic(this);
		}else{
			stackView.logic();
		}
	}

	public void onkeyTyped(char character) {
		if(!ma.update() || !inited)
			return;
		InputController.keyTyped(character);
	}
	
	public void onkeyDown(int keycode) {
		if(!ma.update() || !inited)
			return;
		InputController.keyDown(keycode,this);
	}

	public void onkeyUp(int keycode) {
		if(!ma.update() || !inited)
			return;
		InputController.keyUp(keycode,this);
	}

	public boolean touchDown(int screenX, int screenY, int pointer, int button) {
		InputController.touchDown(screenX, screenY, pointer, button);
		return false;
	}

	@Override
	public boolean touchDragged(int screenX, int screenY, int pointer) {
		InputController.touchDragged(screenX, screenY, pointer);
		return false;
	}
	
	@Override
	public boolean touchUp(int screenX, int screenY, int pointer, int button) {
		InputController.touchUp(screenX, screenY, pointer, button);
		return false;
	}

	@Override
	public boolean scrolled(int amount) {
		InputController.scrolled(amount);
		return false;
	}
	
	public void mouseMoved(int x,int y){
		InputController.mouseMoved(x,y);
	};


}
