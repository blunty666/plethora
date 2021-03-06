package org.squiddev.plethora.gameplay.modules.glasses.methods;

import dan200.computercraft.api.lua.LuaException;
import net.minecraft.item.Item;
import net.minecraft.util.ResourceLocation;
import org.squiddev.plethora.api.method.BasicMethod;
import org.squiddev.plethora.api.method.IContext;
import org.squiddev.plethora.api.method.IUnbakedContext;
import org.squiddev.plethora.api.method.MethodResult;
import org.squiddev.plethora.gameplay.modules.glasses.CanvasServer;
import org.squiddev.plethora.gameplay.modules.glasses.objects.ObjectGroup.Frame2D;
import org.squiddev.plethora.gameplay.modules.glasses.objects.ObjectGroup.Group2D;
import org.squiddev.plethora.gameplay.modules.glasses.objects.object2d.*;
import org.squiddev.plethora.utils.Vec2d;

import static dan200.computercraft.core.apis.ArgumentHelper.getString;
import static dan200.computercraft.core.apis.ArgumentHelper.optInt;
import static org.squiddev.plethora.api.method.ArgumentHelper.getFloat;
import static org.squiddev.plethora.api.method.ArgumentHelper.optFloat;
import static org.squiddev.plethora.gameplay.modules.glasses.methods.ArgumentPointHelper.getVec2d;
import static org.squiddev.plethora.gameplay.modules.glasses.objects.Colourable.DEFAULT_COLOUR;

public class MethodsCanvas2D {
	@BasicMethod.Inject(value = Group2D.class, doc = "function(x:number, y:number, width:number, height:number[, color:number]):table -- Create a new rectangle.")
	public static MethodResult addRectangle(IUnbakedContext<Group2D> context, Object[] args) throws LuaException {
		float x = getFloat(args, 0);
		float y = getFloat(args, 1);
		float width = getFloat(args, 2);
		float height = getFloat(args, 3);
		int colour = optInt(args, 4, DEFAULT_COLOUR);

		IContext<Group2D> baked = context.safeBake();
		Group2D group = baked.getTarget();
		CanvasServer canvas = baked.getContext(CanvasServer.class);

		Rectangle rectangle = new Rectangle(canvas.newObjectId(), group.id());
		rectangle.setPosition(new Vec2d(x, y));
		rectangle.setSize(width, height);
		rectangle.setColour(colour);

		canvas.add(rectangle);

		return MethodResult.result(baked.makeChild(rectangle, rectangle.reference(canvas)).getObject());
	}

	@BasicMethod.Inject(value = Group2D.class, doc = "function(start:table, end:table[, color:number][, thickness:number]):table -- Create a new line.")
	public static MethodResult addLine(IUnbakedContext<Group2D> context, Object[] args) throws LuaException {
		Vec2d start = getVec2d(args, 0);
		Vec2d end = getVec2d(args, 1);
		int colour = optInt(args, 2, DEFAULT_COLOUR);
		float thickness = optFloat(args, 3, 1);

		IContext<Group2D> baked = context.safeBake();
		Group2D group = baked.getTarget();
		CanvasServer canvas = baked.getContext(CanvasServer.class);

		Line line = new Line(canvas.newObjectId(), group.id());
		line.setVertex(0, start);
		line.setVertex(1, end);
		line.setColour(colour);
		line.setScale(thickness);

		canvas.add(line);

		return MethodResult.result(baked.makeChild(line, line.reference(canvas)).getObject());
	}

	@BasicMethod.Inject(value = Group2D.class, doc = "function(position:table, [, color:number][, size:number]):table -- Create a new dot.")
	public static MethodResult addDot(IUnbakedContext<Group2D> context, Object[] args) throws LuaException {
		Vec2d position = getVec2d(args, 0);
		int colour = optInt(args, 1, DEFAULT_COLOUR);
		float size = optFloat(args, 2, 1);

		IContext<Group2D> baked = context.safeBake();
		Group2D group = baked.getTarget();
		CanvasServer canvas = baked.getContext(CanvasServer.class);

		Dot dot = new Dot(canvas.newObjectId(), group.id());
		dot.setPosition(position);
		dot.setColour(colour);
		dot.setScale(size);

		canvas.add(dot);
		return MethodResult.result(baked.makeChild(dot, dot.reference(canvas)).getObject());
	}

	@BasicMethod.Inject(value = Group2D.class, doc = "function(position:table, text:string, [, color:number][, size:number]):table -- Create a new text object.")
	public static MethodResult addText(IUnbakedContext<Group2D> context, Object[] args) throws LuaException {
		Vec2d point = getVec2d(args, 0);
		String contents = getString(args, 1);
		int colour = optInt(args, 2, DEFAULT_COLOUR);
		float size = optFloat(args, 3, 1);

		IContext<Group2D> baked = context.safeBake();
		Group2D group = baked.getTarget();
		CanvasServer canvas = baked.getContext(CanvasServer.class);

		Text text = new Text(canvas.newObjectId(), group.id());
		text.setPosition(point);
		text.setText(contents);
		text.setColour(colour);
		text.setScale(size);

		canvas.add(text);

		return MethodResult.result(baked.makeChild(text, text.reference(canvas)).getObject());
	}

	@BasicMethod.Inject(value = Group2D.class, doc = "function(p1:table, p2:table, p3:table, [, color:number]):table -- Create a new triangle, composed of three points.")
	public static MethodResult addTriangle(IUnbakedContext<Group2D> context, Object[] args) throws LuaException {
		Vec2d a = getVec2d(args, 0);
		Vec2d b = getVec2d(args, 1);
		Vec2d c = getVec2d(args, 2);

		int colour = optInt(args, 3, DEFAULT_COLOUR);

		IContext<Group2D> baked = context.safeBake();
		Group2D group = baked.getTarget();
		CanvasServer canvas = baked.getContext(CanvasServer.class);

		Triangle triangle = new Triangle(canvas.newObjectId(), group.id());
		triangle.setVertex(0, a);
		triangle.setVertex(1, b);
		triangle.setVertex(2, c);
		triangle.setColour(colour);

		canvas.add(triangle);

		return MethodResult.result(baked.makeChild(triangle, triangle.reference(canvas)).getObject());
	}

	@BasicMethod.Inject(value = Group2D.class, doc = "function(points...:table, [, color:number]):table -- Create a new polygon, composed of many points.")
	public static MethodResult addPolygon(IUnbakedContext<Group2D> context, Object[] args) throws LuaException {
		IContext<Group2D> baked = context.safeBake();
		Group2D group = baked.getTarget();
		CanvasServer canvas = baked.getContext(CanvasServer.class);

		Polygon polygon = new Polygon(canvas.newObjectId(), group.id());
		int i;
		for (i = 0; i < args.length; i++) {
			Object arg = args[i];
			if (i >= args.length - 1 && arg instanceof Number) {
				break;
			} else {
				polygon.addPoint(i, getVec2d(args, i));
			}
		}

		polygon.setColour(optInt(args, i, DEFAULT_COLOUR));

		canvas.add(polygon);
		return MethodResult.result(baked.makeChild(polygon, polygon.reference(canvas)).getObject());
	}

	@BasicMethod.Inject(value = Group2D.class, doc = "function(points...:table, [, color:number[, thickness:number]]):table -- Create a new line loop, composed of many points.")
	public static MethodResult addLines(IUnbakedContext<Group2D> context, Object[] args) throws LuaException {
		IContext<Group2D> baked = context.safeBake();
		Group2D group = baked.getTarget();
		CanvasServer canvas = baked.getContext(CanvasServer.class);

		LineLoop lines = new LineLoop(canvas.newObjectId(), group.id());
		int i;
		for (i = 0; i < args.length; i++) {
			Object arg = args[i];
			if (i >= args.length - 2 && arg instanceof Number) {
				break;
			} else {
				lines.addPoint(i, getVec2d(args, i));
			}
		}

		lines.setColour(optInt(args, i, DEFAULT_COLOUR));
		lines.setScale(optFloat(args, i + 1, 1));

		canvas.add(lines);
		return MethodResult.result(baked.makeChild(lines, lines.reference(canvas)).getObject());
	}

	@BasicMethod.Inject(value = Group2D.class, doc = "function(position:table, id:string[, damage:number][, scale:number]):table -- Create a item icon.")
	public static MethodResult addItem(IUnbakedContext<Group2D> context, Object[] args) throws LuaException {
		Vec2d position = getVec2d(args, 0);
		ResourceLocation name = new ResourceLocation(getString(args, 1));
		int damage = optInt(args, 2, 0);
		float scale = optFloat(args, 3, 1);

		Item item = Item.REGISTRY.getObject(name);
		if (item == null) throw new LuaException("Unknown item '" + name + "'");

		IContext<Group2D> baked = context.safeBake();
		Group2D group = baked.getTarget();
		CanvasServer canvas = baked.getContext(CanvasServer.class);

		Item2D model = new Item2D(canvas.newObjectId(), group.id());
		model.setPosition(position);
		model.setScale(scale);
		model.setItem(item);
		model.setDamage(damage);

		canvas.add(model);
		return MethodResult.result(baked.makeChild(model, model.reference(canvas)).getObject());
	}

	@BasicMethod.Inject(value = Group2D.class, doc = "function(position:table):table -- Create a new object group.")
	public static MethodResult addGroup(IUnbakedContext<Group2D> context, Object[] args) throws LuaException {
		Vec2d position = getVec2d(args, 0);

		IContext<Group2D> baked = context.safeBake();
		Group2D group = baked.getTarget();
		CanvasServer canvas = baked.getContext(CanvasServer.class);

		ObjectGroup2D newGroup = new ObjectGroup2D(canvas.newObjectId(), group.id());
		newGroup.setPosition(position);

		canvas.add(newGroup);
		return MethodResult.result(baked.makeChild(newGroup, newGroup.reference(canvas)).getObject());
	}

	@BasicMethod.Inject(value = Frame2D.class, doc = "function():number, number -- Get the size of this canvas.")
	public static MethodResult getSize(IUnbakedContext<Frame2D> context, Object[] args) throws LuaException {
		Frame2D target = context.safeBake().getTarget();
		return MethodResult.result(target.getWidth(), target.getHeight());
	}
}
