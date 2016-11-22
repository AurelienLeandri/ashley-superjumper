/*******************************************************************************
 * Copyright 2014 See AUTHORS file.
 * 
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 * 
 *   http://www.apache.org/licenses/LICENSE-2.0
 * 
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 ******************************************************************************/

package com.siondream.superjumper.systems;

import com.badlogic.ashley.core.ComponentMapper;
import com.badlogic.ashley.core.Entity;
import com.badlogic.ashley.core.Family;
import com.badlogic.ashley.systems.IteratingSystem;
import com.siondream.superjumper.World;
import com.siondream.superjumper.components.BobComponent;
import com.siondream.superjumper.components.MovementComponent;
import com.siondream.superjumper.components.TransformComponent;
import com.siondream.superjumper.components.StateComponent;


/*
README: Réflexion générale sur les systems. Bon ça a l'air ok ce qu'il fait du pdv d'Ashley. Nous on voudrait
soit faire deux moteurs, un pour l'ui avec scene2D le scene graph, et le jeu avec les components fin je l'ai
compris comme ça pour le moment.

Je pense toujours à ma première solution pour éviter ça:
Une family NestedEntity qui contient: (*voir plus bas)
- Un vecteur d'Entity& children
- Un vecteur d'Entity& parents
Associé à ça, un NestedEntitySystem qui contient une fonction abstraite UpdateRelated(): Dans les systems fils,
elle met à jour les Entity sur la base des data des parents (ex basePos.x = parent.basePos.x).
On l'appelle dans le update du NestedEntitySystem en tête de fonction, avant quoi que ce soit.
Ca marche sûr pour les déplacements et c'est suffisamment souple pour même pouvoir faire des trucs un
peu plus compliqués. Coût de travail ? l'overrider dans les systems qui en ont besoin en trois lignes.
Résultats:
    On peut utiliser le component-based pour l'ui en gardant le principe du scenegraph. On mélange pas avec
    LibGDX.
    On peut utiliser le principe du scenegraph in-game en gardant le principe du component based et faire
    des objets composites.
    On est contents.
    On suce Dvide.
MAIIIIIS: Je connais pas bien libgdx et je suis sûr qu'en haut (*) ya des objets dans libgdx qui gèrent ça mieux
que des vecteurs. Reste à voir si on peut les appliquer.
 */

public class BobSystem extends IteratingSystem {
	private static final Family family = Family.all(BobComponent.class,
													   StateComponent.class,
													   TransformComponent.class,
													   MovementComponent.class).get();
	
	private float accelX = 0.0f;
	private World world;
	
	private ComponentMapper<BobComponent> bm;
	private ComponentMapper<StateComponent> sm;
	private ComponentMapper<TransformComponent> tm;
	private ComponentMapper<MovementComponent> mm;
	
	public BobSystem(World world) {
		super(family);
		
		this.world = world;
		
		bm = ComponentMapper.getFor(BobComponent.class);
		sm = ComponentMapper.getFor(StateComponent.class);
		tm = ComponentMapper.getFor(TransformComponent.class);
		mm = ComponentMapper.getFor(MovementComponent.class);
	}
	
	public void setAccelX(float accelX) {
		this.accelX = accelX;
	}
	
	@Override
	public void update(float deltaTime) {
		super.update(deltaTime);
		
		accelX = 0.0f;
	}
	
	@Override
	public void processEntity(Entity entity, float deltaTime) {
		TransformComponent t = tm.get(entity);
		StateComponent state = sm.get(entity);
		MovementComponent mov = mm.get(entity);
		BobComponent bob = bm.get(entity);
		
		if (state.get() != BobComponent.STATE_HIT && t.pos.y <= 0.5f) {
			hitPlatform(entity);
		}
		
		if (state.get() != BobComponent.STATE_HIT) {
			mov.velocity.x = -accelX / 10.0f * BobComponent.MOVE_VELOCITY;
		}
		
		if (mov.velocity.y > 0 && state.get() != BobComponent.STATE_HIT) {
			if (state.get() != BobComponent.STATE_JUMP) {
				state.set(BobComponent.STATE_JUMP);
			}
		}

		if (mov.velocity.y < 0 && state.get() != BobComponent.STATE_HIT) {
			if (state.get() != BobComponent.STATE_FALL) {
				state.set(BobComponent.STATE_FALL);
			}
		}

		if (t.pos.x < 0) {
			t.pos.x = World.WORLD_WIDTH;
		}
		
		if (t.pos.x > World.WORLD_WIDTH) {
			t.pos.x = 0;
		}
		
		t.scale.x = mov.velocity.x < 0.0f ? Math.abs(t.scale.x) * -1.0f : Math.abs(t.scale.x);
		
		bob.heightSoFar = Math.max(t.pos.y, bob.heightSoFar);
		
		if (bob.heightSoFar - 7.5f > t.pos.y) {
			world.state = World.WORLD_STATE_GAME_OVER;
		}
	}
	
	public void hitSquirrel (Entity entity) {
		if (!family.matches(entity)) return;
		
		StateComponent state = sm.get(entity);
		MovementComponent mov = mm.get(entity);
		
		mov.velocity.set(0, 0);
		state.set(BobComponent.STATE_HIT);
	}

	public void hitPlatform (Entity entity) {
		if (!family.matches(entity)) return;
		
		StateComponent state = sm.get(entity);
		MovementComponent mov = mm.get(entity);
		
		mov.velocity.y = BobComponent.JUMP_VELOCITY;
		state.set(BobComponent.STATE_JUMP);
	}

	public void hitSpring (Entity entity) {
		if (!family.matches(entity)) return;
		
		StateComponent state = sm.get(entity);
		MovementComponent mov = mm.get(entity);
		
		mov.velocity.y = BobComponent.JUMP_VELOCITY * 1.5f;
		state.set(BobComponent.STATE_JUMP);
	}
}
