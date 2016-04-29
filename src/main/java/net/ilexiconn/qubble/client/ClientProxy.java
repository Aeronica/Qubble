package net.ilexiconn.qubble.client;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import net.ilexiconn.llibrary.client.model.qubble.QubbleCube;
import net.ilexiconn.llibrary.client.model.qubble.QubbleModel;
import net.ilexiconn.qubble.client.model.BlockModelContainer;
import net.ilexiconn.qubble.client.world.DummyWorld;
import net.ilexiconn.qubble.server.ServerProxy;
import net.ilexiconn.qubble.server.model.importer.IModelImporter;
import net.ilexiconn.qubble.server.model.importer.ModelImporters;
import net.minecraft.block.state.IBlockState;
import net.minecraft.client.Minecraft;
import net.minecraft.client.model.*;
import net.minecraft.client.renderer.block.model.ModelBlockDefinition;
import net.minecraft.client.renderer.block.model.ModelResourceLocation;
import net.minecraft.client.renderer.block.model.Variant;
import net.minecraft.client.renderer.block.model.VariantList;
import net.minecraft.client.renderer.block.model.multipart.Multipart;
import net.minecraft.client.renderer.block.model.multipart.Selector;
import net.minecraft.client.renderer.entity.Render;
import net.minecraft.client.renderer.tileentity.TileEntityRendererDispatcher;
import net.minecraft.client.renderer.tileentity.TileEntitySpecialRenderer;
import net.minecraft.client.resources.IResourceManager;
import net.minecraft.entity.Entity;
import net.minecraft.entity.EntityLivingBase;
import net.minecraft.tileentity.TileEntity;
import net.minecraft.util.ResourceLocation;
import net.minecraft.world.World;
import net.minecraftforge.client.model.BlockStateLoader;
import net.minecraftforge.common.MinecraftForge;
import net.minecraftforge.fml.relauncher.Side;
import net.minecraftforge.fml.relauncher.SideOnly;

import java.io.File;
import java.io.IOException;
import java.io.InputStreamReader;
import java.lang.reflect.Field;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.lang.reflect.Modifier;
import java.util.*;

@SideOnly(Side.CLIENT)
public class ClientProxy extends ServerProxy {
    public static final int QUBBLE_BUTTON_ID = "QUBBLE_BUTTON_ID".hashCode();
    public static final Minecraft MINECRAFT = Minecraft.getMinecraft();
    public static final File QUBBLE_DIRECTORY = new File(".", "llibrary" + File.separator + "qubble");
    public static final File QUBBLE_MODEL_DIRECTORY = new File(QUBBLE_DIRECTORY, "models");
    public static final File QUBBLE_TEXTURE_DIRECTORY = new File(QUBBLE_DIRECTORY, "textures");
    public static final File QUBBLE_EXPORT_DIRECTORY = new File(QUBBLE_DIRECTORY, "exports");
    public static final Map<String, QubbleModel> GAME_MODELS = new HashMap<>();
    public static final Map<String, QubbleModel> GAME_JSON_MODELS = new HashMap<>();
    public static final Map<String, ResourceLocation> GAME_TEXTURES = new HashMap<>();
    private static Field TEXTURE_QUADS_FIELD;
    private static Method GET_ENTITY_TEXTURE_METHOD;

    @Override
    public void onPreInit() {
        super.onPreInit();
        MinecraftForge.EVENT_BUS.register(ClientEventHandler.INSTANCE);
        if (!QUBBLE_MODEL_DIRECTORY.exists()) {
            QUBBLE_MODEL_DIRECTORY.mkdirs();
        }
        if (!QUBBLE_TEXTURE_DIRECTORY.exists()) {
            QUBBLE_TEXTURE_DIRECTORY.mkdirs();
        }
        if (!QUBBLE_EXPORT_DIRECTORY.exists()) {
            QUBBLE_EXPORT_DIRECTORY.mkdirs();
        }
        for (Field field : ModelBox.class.getDeclaredFields()) {
            if (field.getType() == TexturedQuad[].class) {
                field.setAccessible(true);
                TEXTURE_QUADS_FIELD = field;
                break;
            }
        }
        for (Method method : Render.class.getDeclaredMethods()) {
            if (method.getReturnType().equals(ResourceLocation.class) && Modifier.isAbstract(method.getModifiers()) && method.getParameterTypes().length == 1) {
                method.setAccessible(true);
                GET_ENTITY_TEXTURE_METHOD = method;
                break;
            }
        }
    }

    @Override
    public void onInit() {
        super.onInit();
    }

    @Override
    public void onPostInit() {
        super.onPostInit();
        for (Map.Entry<Class<? extends Entity>, Render<? extends Entity>> entry : MINECRAFT.getRenderManager().entityRenderMap.entrySet()) {
            Render<? extends Entity> renderer = entry.getValue();
            String entityName = entry.getKey().getSimpleName().replaceAll("Entity", "");
            Entity entity = null;
            try {
                entity = entry.getKey().getConstructor(World.class).newInstance(new DummyWorld());
                entityName = entity.getName();
            } catch (Exception e) {
            }
            for (Field field : this.getAllFields(renderer.getClass())) {
                try {
                    if (ModelBase.class.isAssignableFrom(field.getType())) {
                        field.setAccessible(true);
                        QubbleModel model = this.parseModel((ModelBase) field.get(renderer), entry.getKey(), entityName);
                        if (model.getCubes().size() > 0) {
                            GAME_MODELS.put(entityName, model);
                        }
                    } else if (ResourceLocation[].class.isAssignableFrom(field.getType()) && !GAME_TEXTURES.containsKey(entityName)) {
                        field.setAccessible(true);
                        ResourceLocation[] textures = (ResourceLocation[]) field.get(renderer);
                        if (textures.length > 0) {
                            ResourceLocation texture = textures[0];
                            if (!texture.toString().contains("shadow")) {
                                GAME_TEXTURES.put(entityName, texture);
                            }
                        }
                    } else if (ResourceLocation.class.isAssignableFrom(field.getType()) && !GAME_TEXTURES.containsKey(entityName)) {
                        field.setAccessible(true);
                        ResourceLocation texture = (ResourceLocation) field.get(renderer);
                        if (!texture.toString().contains("shadow")) {
                            GAME_TEXTURES.put(entityName, texture);
                        }
                    }
                } catch (Exception e) {
                    System.err.println("Failed to load model from " + renderer.getClass() + "#" + field.getName());
                    e.printStackTrace();
                }
            }
            if (entity != null) {
                try {
                    ResourceLocation texture = (ResourceLocation) GET_ENTITY_TEXTURE_METHOD.invoke(renderer, entity);
                    if (texture != null) {
                        GAME_TEXTURES.put(entityName, texture);
                    }
                } catch (Exception e) {
                }
            }
        }
        for (Map.Entry<Class<? extends TileEntity>, TileEntitySpecialRenderer<? extends TileEntity>> entry : TileEntityRendererDispatcher.instance.mapSpecialRenderers.entrySet()) {
            TileEntitySpecialRenderer<? extends TileEntity> renderer = entry.getValue();
            String tileName = entry.getKey().getSimpleName();
            for (Field field : this.getAllFields(renderer.getClass())) {
                try {
                    if (ModelBase.class.isAssignableFrom(field.getType())) {
                        field.setAccessible(true);
                        QubbleModel model = this.parseModel((ModelBase) field.get(renderer), entry.getKey(), tileName);
                        if (model.getCubes().size() > 0) {
                            GAME_MODELS.put(tileName, model);
                        }
                    } else if (ResourceLocation[].class.isAssignableFrom(field.getType()) && !GAME_TEXTURES.containsKey(tileName)) {
                        field.setAccessible(true);
                        ResourceLocation[] textures = (ResourceLocation[]) field.get(renderer);
                        if (textures.length > 0) {
                            ResourceLocation texture = textures[0];
                            if (!texture.toString().contains("destroy_stage")) {
                                GAME_TEXTURES.put(tileName, texture);
                            }
                        }
                    } else if (ResourceLocation.class.isAssignableFrom(field.getType()) && !GAME_TEXTURES.containsKey(tileName)) {
                        field.setAccessible(true);
                        ResourceLocation texture = (ResourceLocation) field.get(renderer);
                        if (!texture.toString().contains("destroy_stage")) {
                            GAME_TEXTURES.put(tileName, texture);
                        }
                    }
                } catch (Exception e) {
                    System.err.println("Failed to load model from " + renderer.getClass() + "#" + field.getName());
                    e.printStackTrace();
                }
            }
        }
        Map<IBlockState, ModelResourceLocation> blockModels = MINECRAFT.getRenderItem().getItemModelMesher().getModelManager().getBlockModelShapes().getBlockStateMapper().putAllStateModelLocations();
        Gson gson = (new GsonBuilder()).registerTypeAdapter(ModelBlockDefinition.class, new ModelBlockDefinition.Deserializer()).registerTypeAdapter(Variant.class, new Variant.Deserializer()).registerTypeAdapter(VariantList.class, new VariantList.Deserializer()).registerTypeAdapter(Multipart.class, new Multipart.Deserializer()).registerTypeAdapter(Selector.class, new Selector.Deserializer()).create();
        IResourceManager resourceManager = MINECRAFT.getResourceManager();
        IModelImporter<BlockModelContainer> importer = (IModelImporter<BlockModelContainer>) ModelImporters.BLOCK_JSON.getModelImporter();
        for (Map.Entry<IBlockState, ModelResourceLocation> entry : blockModels.entrySet()) {
            String name = entry.getKey().getBlock().getLocalizedName();
            ModelResourceLocation modelResource = entry.getValue();
            try {
                ResourceLocation blockStateResource = new ResourceLocation(modelResource.getResourceDomain(), "blockstates/" + modelResource.getResourcePath() + ".json");
                ModelBlockDefinition state = BlockStateLoader.load(new InputStreamReader(resourceManager.getResource(blockStateResource).getInputStream()), gson);
                outer:
                for (VariantList variantList : state.getMultipartVariants()) {
                    for (Variant variant : variantList.getVariantList()) {
                        ResourceLocation resource = variant.getModelLocation();
                        QubbleModel qubbleModel = parseJsonModel(gson, resourceManager, importer, name, new ResourceLocation(resource.getResourceDomain(), "models/" + resource.getResourcePath() + ".json"));
                        if (qubbleModel != null) {
                            GAME_JSON_MODELS.put(name, qubbleModel);
                        }
                        break outer;
                    }
                }
            } catch (IOException e) {
                System.err.println(e.toString());
            }
        }
    }

    private QubbleModel parseJsonModel(Gson gson, IResourceManager resourceManager, IModelImporter<BlockModelContainer> importer, String name, ResourceLocation resource) throws IOException {
        BlockModelContainer model = gson.fromJson(new InputStreamReader(resourceManager.getResource(resource).getInputStream()), BlockModelContainer.class);
        if (model.parent == null || !model.parent.contains("cube_all")) {
            QubbleModel qubbleModel = importer.getModel(name, model);
            if (model.parent != null) {
                ResourceLocation parentResource = new ResourceLocation(model.parent);
                QubbleModel parent = this.parseJsonModel(gson, resourceManager, importer, name, new ResourceLocation(parentResource.getResourceDomain(), "models/" + parentResource.getResourcePath() + ".json"));
                if (parent != null) {
                    qubbleModel.getCubes().addAll(parent.getCubes());
                }
            }
            if (qubbleModel.getCubes().size() > 0) {
                if (qubbleModel.getCubes().size() == 1) {
                    QubbleCube cube = qubbleModel.getCubes().get(0);
                    if (cube.getDimensionX() == 16.0F && cube.getDimensionY() == 16.0F && cube.getDimensionZ() == 16.0F) {
                        return null;
                    }
                }
                return qubbleModel;
            }
        }
        return null;
    }

    private List<Field> getAllFields(Class<?> clazz) {
        List<Field> fields = new LinkedList<>();
        Collections.addAll(fields, clazz.getDeclaredFields());
        if (clazz.getSuperclass() != null && clazz.getSuperclass() != Object.class) {
            fields.addAll(this.getAllFields(clazz.getSuperclass()));
        }
        return fields;
    }

    private QubbleModel parseModel(ModelBase model, Class<?> clazz, String name) {
        QubbleModel qubbleModel = QubbleModel.create(name, "Unknown", model.textureWidth, model.textureHeight);
        if (clazz != null && Entity.class.isAssignableFrom(clazz)) {
            try {
                Entity entity = (Entity) clazz.getConstructor(World.class).newInstance(new DummyWorld());
                try {
                    model.setRotationAngles(0.0F, 0.0F, 0.0F, 0.0F, 0.0F, 0.0625F, entity);
                } catch (Exception e) {
                }
                if (entity instanceof EntityLivingBase) {
                    try {
                        model.setLivingAnimations((EntityLivingBase) entity, 0.0F, 0.0F, 0.0F);
                    } catch (Exception e) {
                    }
                }
                try {
                    model.render(entity, 0.0F, 0.0F, 0.0F, 0.0F, 0.0F, 0.0625F);
                } catch (Exception e) {
                }
            } catch (NoSuchMethodException | IllegalAccessException | InstantiationException | InvocationTargetException e) {
            }
        }
        Map<String, ModelRenderer> cuboidsWithNames = this.getCuboidsWithNames(model);
        for (Map.Entry<String, ModelRenderer> entry : cuboidsWithNames.entrySet()) {
            ModelRenderer modelRenderer = entry.getValue();
            if (modelRenderer != null && modelRenderer.cubeList != null) {
                qubbleModel.setTextureWidth((int) modelRenderer.textureWidth);
                qubbleModel.setTextureHeight((int) modelRenderer.textureHeight);
                break;
            }
        }
        for (Map.Entry<String, ModelRenderer> entry : cuboidsWithNames.entrySet()) {
            qubbleModel.getCubes().addAll(parseModelRenderer(model, qubbleModel, entry.getKey(), entry.getValue(), null));
        }
        return qubbleModel;
    }

    private List<QubbleCube> parseModelRenderer(ModelBase model, QubbleModel qubbleModel, String name, ModelRenderer modelRenderer, QubbleCube parent) {
        List<QubbleCube> cubes = new ArrayList<>();
        int boxIndex = 0;
        if (modelRenderer != null && modelRenderer.cubeList != null) {
            for (ModelBox box : modelRenderer.cubeList) {
                float textureWidth = qubbleModel.getTextureWidth();
                float textureHeight = qubbleModel.getTextureHeight();
                if (modelRenderer.textureWidth != 64 || modelRenderer.textureHeight != 32) {
                    textureWidth = modelRenderer.textureWidth;
                    textureHeight = modelRenderer.textureHeight;
                }
                QubbleCube cube = QubbleCube.create((modelRenderer.boxName != null ? modelRenderer.boxName : name) + (boxIndex != 0 ? box.boxName != null ? box.boxName : "_" + boxIndex : ""));
                cube.setPosition(modelRenderer.rotationPointX, modelRenderer.rotationPointY, modelRenderer.rotationPointZ);
                cube.setRotation((float) Math.toDegrees(modelRenderer.rotateAngleX), (float) Math.toDegrees(modelRenderer.rotateAngleY), (float) Math.toDegrees(modelRenderer.rotateAngleZ));
                cube.setOffset(box.posX1, box.posY1, box.posZ1);
                cube.setDimensions((int) Math.abs(box.posX2 - box.posX1), (int) Math.abs(box.posY2 - box.posY1), (int) Math.abs(box.posZ2 - box.posZ1));
                TextureOffset textureOffset = model.getTextureOffset(box.boxName);
                if (textureOffset != null) {
                    cube.setTexture(textureOffset.textureOffsetX, textureOffset.textureOffsetY);
                } else {
                    TexturedQuad[] quads = this.getTexturedQuads(box);
                    if (quads != null) {
                        PositionTextureVertex[] vertices = quads[1].vertexPositions;
                        cube.setTextureMirrored((vertices[2].vector3D.yCoord - vertices[0].vector3D.yCoord - cube.getDimensionY()) / 2.0F < 0.0F);
                        if (vertices[cube.isTextureMirrored() ? 2 : 1].texturePositionY > vertices[cube.isTextureMirrored() ? 1 : 2].texturePositionY) {
                            cube.setTextureMirrored(!cube.isTextureMirrored());
                        }
                        cube.setTexture((int) (vertices[cube.isTextureMirrored() ? 2 : 1].texturePositionX * textureWidth), (int) ((vertices[cube.isTextureMirrored() ? 2 : 1].texturePositionY * textureHeight) - cube.getDimensionZ()));
                    }
                }
                boxIndex++;
                cubes.add(cube);
            }
            if (cubes.size() > 0 && modelRenderer.childModels != null) {
                int i = 0;
                for (ModelRenderer child : modelRenderer.childModels) {
                    this.parseModelRenderer(model, qubbleModel, child.boxName != null ? child.boxName : name + "_" + i, child, cubes.get(0));
                    i++;
                }
            }
        }
        if (parent != null) {
            parent.getChildren().addAll(cubes);
        }
        return cubes;
    }

    private Map<String, ModelRenderer> getCuboidsWithNames(ModelBase model) {
        Map<String, ModelRenderer> cuboids = new HashMap<>();
        for (Field field : this.getAllFields(model.getClass())) {
            try {
                if (ModelRenderer.class.isAssignableFrom(field.getType())) {
                    field.setAccessible(true);
                    ModelRenderer modelRenderer = (ModelRenderer) field.get(model);
                    if (modelRenderer != null) {
                        cuboids.put(field.getName(), modelRenderer);
                    }
                } else if (ModelRenderer[].class.isAssignableFrom(field.getType())) {
                    field.setAccessible(true);
                    ModelRenderer[] boxes = (ModelRenderer[]) field.get(model);
                    if (boxes != null) {
                        for (int i = 0; i < boxes.length; i++) {
                            cuboids.put(field.getName() + "_" + i, boxes[i]);
                        }
                    }
                } else if (List.class.isAssignableFrom(field.getType())) {
                    if (field.getDeclaringClass() != ModelBase.class) {
                        field.setAccessible(true);
                        List boxes = (List) field.get(model);
                        if (boxes != null) {
                            for (int i = 0; i < boxes.size(); i++) {
                                Object obj = boxes.get(i);
                                if (obj instanceof ModelRenderer) {
                                    cuboids.put(field.getName() + "_" + i, (ModelRenderer) obj);
                                }
                            }
                        }
                    }
                }
            } catch (IllegalAccessException e) {
            }
        }
        return cuboids;
    }

    private TexturedQuad[] getTexturedQuads(ModelBox box) {
        try {
            return (TexturedQuad[]) TEXTURE_QUADS_FIELD.get(box);
        } catch (IllegalAccessException e) {
            e.printStackTrace();
        }
        return null;
    }

    public static List<String> getGameModels() {
        List<String> gameModels = new LinkedList<>();
        for (Map.Entry<String, QubbleModel> entry : GAME_MODELS.entrySet()) {
            gameModels.add(entry.getKey());
        }
        Collections.sort(gameModels);
        return gameModels;
    }

    public static List<String> getGameBlockModels() {
        List<String> gameModels = new LinkedList<>();
        for (Map.Entry<String, QubbleModel> entry : GAME_JSON_MODELS.entrySet()) {
            gameModels.add(entry.getKey());
        }
        Collections.sort(gameModels);
        return gameModels;
    }
}
