package com.finpro7.oop.entities;

import com.badlogic.gdx.graphics.g3d.Model;
import com.badlogic.gdx.graphics.g3d.ModelInstance;
import com.badlogic.gdx.graphics.g3d.utils.AnimationController;

public class Yajuj extends BaseEnemy {

    public Yajuj(Model model) {
        this.modelInstance = new ModelInstance(model);
        this.animController = new AnimationController(modelInstance);
    }
}
