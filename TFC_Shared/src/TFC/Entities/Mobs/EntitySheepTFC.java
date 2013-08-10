package TFC.Entities.Mobs;

import java.util.ArrayList;
import java.util.Random;

import net.minecraft.entity.EntityAgeable;
import net.minecraft.entity.EntityLiving;
import net.minecraft.entity.SharedMonsterAttributes;
import net.minecraft.entity.ai.EntityAIEatGrass;
import net.minecraft.entity.ai.EntityAIFollowParent;
import net.minecraft.entity.ai.EntityAILookIdle;
import net.minecraft.entity.ai.EntityAIPanic;
import net.minecraft.entity.ai.EntityAISwimming;
import net.minecraft.entity.ai.EntityAITempt;
import net.minecraft.entity.ai.EntityAIWander;
import net.minecraft.entity.ai.EntityAIWatchClosest;
import net.minecraft.entity.passive.EntitySheep;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.item.ItemStack;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.util.MathHelper;
import net.minecraft.world.World;
import net.minecraftforge.common.IShearable;
import TFC.TFCItems;
import TFC.API.Entities.IAnimal;
import TFC.Core.TFC_Settings;
import TFC.Core.TFC_Time;
import TFC.Entities.AI.EntityAIMateTFC;
import cpw.mods.fml.relauncher.Side;
import cpw.mods.fml.relauncher.SideOnly;

public class EntitySheepTFC extends EntitySheep implements IShearable, IAnimal
{
	/**
	 * Holds the RGB table of the sheep colors - in OpenGL glColor3f values - used to render the sheep colored fleece.
	 */
	public static final float[][] fleeceColorTable = new float[][] {{1.0F, 1.0F, 1.0F}, {0.95F, 0.7F, 0.2F}, {0.9F, 0.5F, 0.85F}, {0.6F, 0.7F, 0.95F}, {0.9F, 0.9F, 0.2F}, {0.5F, 0.8F, 0.1F}, {0.95F, 0.7F, 0.8F}, {0.3F, 0.3F, 0.3F}, {0.6F, 0.6F, 0.6F}, {0.3F, 0.6F, 0.7F}, {0.7F, 0.4F, 0.9F}, {0.2F, 0.4F, 0.8F}, {0.5F, 0.4F, 0.3F}, {0.4F, 0.5F, 0.2F}, {0.8F, 0.3F, 0.3F}, {0.1F, 0.1F, 0.1F}};

	/**
	 * Used to control movement as well as wool regrowth. Set to 40 on handleHealthUpdate and counts down with each
	 * tick.
	 */
	private int sheepTimer;

	/** The eat grass AI task for this mob. */
	public EntityAIEatGrass aiEatGrass = new EntityAIEatGrass(this);

	protected long animalID;
	protected int sex;
	protected int hunger;
	protected long hasMilkTime;
	protected int age;
	protected boolean pregnant;
	protected int pregnancyTime;
	protected long conception;
	protected float mateSizeMod;
	public float size_mod;
	public boolean inLove;

	public EntitySheepTFC(World par1World)
	{
		super(par1World);
		this.setSize(0.9F, 1.3F);
		float var2 = 0.23F;
		this.getNavigator().setAvoidsWater(true);
		this.tasks.addTask(0, new EntityAISwimming(this));
		this.tasks.addTask(1, new EntityAIPanic(this, 0.38F));
		this.tasks.addTask(2, new EntityAIMateTFC(this,worldObj, var2));
		this.tasks.addTask(3, new EntityAITempt(this, 0.25F, TFCItems.WheatGrain.itemID, false));
		this.tasks.addTask(4, new EntityAIFollowParent(this, 0.25F));
		this.tasks.addTask(6, this.aiEatGrass);
		this.tasks.addTask(5, new EntityAIWander(this, var2));
		this.tasks.addTask(7, new EntityAIWatchClosest(this, EntityPlayer.class, 6.0F));
		this.tasks.addTask(8, new EntityAILookIdle(this));
	}
	public EntitySheepTFC(World par1World,IAnimal mother, float F_size)
	{
		super(par1World);
		size_mod = (((rand.nextInt (4+1)*(rand.nextBoolean()?1:-1)) / 10f) + 1F) * (1.0F - 0.1F * sex) * (float)Math.sqrt((mother.getSize() + F_size)/1.9F);
		size_mod = Math.min(Math.max(size_mod, 0.7F),1.3f);
	}

	@Override
	protected void func_110147_ax()
	{
		super.func_110147_ax();
		this.func_110148_a(SharedMonsterAttributes.field_111267_a).func_111128_a(400);//MaxHealth
	}

	/**
	 * Returns true if the newer Entity AI code should be run
	 */
	@Override
	protected boolean isAIEnabled()
	{
		return true;
	}
	@Override
	protected void updateAITasks()
	{
		this.sheepTimer = this.aiEatGrass.getEatGrassTick();
		super.updateAITasks();
	}

	/**
	 * Called frequently so the entity can update its state every tick as required. For example, zombies and skeletons
	 * use this to react to sunlight and start to burn.
	 */
	@Override
	public void onLivingUpdate()
	{
		if (this.worldObj.isRemote)
		{
			this.sheepTimer = Math.max(0, this.sheepTimer - 1);
		}

		float negDay = -TFC_Settings.dayLength;
		float ratio = TFC_Time.getYearRatio();
		//float t = (1.0F-(getGrowingAge()/(ratio * adultAge * negDay)));
		//float t = (1.0F-(getGrowingAge()/(-24000*adultAge)));
		if(pregnant){
			if(TFC_Time.getTotalTicks() >= conception + pregnancyTime*TFC_Settings.dayLength){
				int i = rand.nextInt(3) + 1;
				for (int x = 0; x<i;x++){
					EntitySheepTFC baby = new EntitySheepTFC(worldObj, this,mateSizeMod);
					//giveBirth(baby);
				}
				pregnant = false;
			}
		}

		super.onLivingUpdate();
	}

	/**
	 * Drop 0-2 items of this living's type
	 */
	@Override
	protected void dropFewItems(boolean par1, int par2)
	{
		if(this.isAdult()){
			if(!this.getSheared()) {
				this.entityDropItem(new ItemStack(TFCItems.SheepSkin,1), 0.0F);
			} else {
				this.dropItem(TFCItems.Hide.itemID,1);
			}
		}

		if (this.isBurning()) {
			this.dropItem(TFCItems.muttonCooked.itemID,(int)(size_mod*(5+rand.nextInt(5))));
		} else {
			this.dropItem(TFCItems.muttonRaw.itemID,(int)(size_mod*(5+rand.nextInt(5))));
		}
	}

	/**
	 * Returns the item ID for the item the mob drops on death.
	 */
	@Override
	protected int getDropItemId()
	{
		return TFCItems.Wool.itemID;
	}

	@SideOnly(Side.CLIENT)
	@Override
	public void handleHealthUpdate(byte par1)
	{
		if (par1 == 10)
		{
			this.sheepTimer = 40;
		}
		else
		{
			super.handleHealthUpdate(par1);
		}
	}

	@SideOnly(Side.CLIENT)
	public float func_44003_c(float par1)
	{
		return this.sheepTimer <= 0 ? 0.0F : (this.sheepTimer >= 4 && this.sheepTimer <= 36 ? 1.0F : (this.sheepTimer < 4 ? (this.sheepTimer - par1) / 4.0F : -(this.sheepTimer - 40 - par1) / 4.0F));
	}

	@SideOnly(Side.CLIENT)
	public float func_44002_d(float par1)
	{
		if (this.sheepTimer > 4 && this.sheepTimer <= 36)
		{
			float var2 = (this.sheepTimer - 4 - par1) / 32.0F;
			return ((float)Math.PI / 5F) + ((float)Math.PI * 7F / 100F) * MathHelper.sin(var2 * 28.7F);
		}
		else
		{
			return this.sheepTimer > 0 ? ((float)Math.PI / 5F) : this.rotationPitch / (180F / (float)Math.PI);
		}
	}

	/**
	 * Called when a player interacts with a mob. e.g. gets milk from a cow, gets into the saddle on a pig.
	 */
	@Override
	public boolean interact(EntityPlayer par1EntityPlayer)
	{

		if(getGender() == GenderEnum.FEMALE && isAdult() && hasMilkTime < TFC_Time.getTotalTicks()){
			ItemStack var2 = par1EntityPlayer.inventory.getCurrentItem();
			if (var2 != null && var2.itemID == TFCItems.WoodenBucketEmpty.itemID) {
				par1EntityPlayer.inventory.setInventorySlotContents(par1EntityPlayer.inventory.currentItem, new ItemStack(TFCItems.WoodenBucketMilk));
				hasMilkTime = TFC_Time.getTotalTicks() + (3*TFC_Time.dayLength); //Can be milked ones every 3 days
				return true;
			}
		}
		return super.interact(par1EntityPlayer);
	}

	/**
	 * (abstract) Protected helper method to write subclass entity data to NBT.
	 */
	@Override
	public void writeEntityToNBT(NBTTagCompound par1NBTTagCompound)
	{
		super.writeEntityToNBT(par1NBTTagCompound);
		par1NBTTagCompound.setBoolean("Sheared", this.getSheared());
		par1NBTTagCompound.setByte("Color", (byte)this.getFleeceColor());
	}

	/**
	 * (abstract) Protected helper method to read subclass entity data from NBT.
	 */
	@Override
	public void readEntityFromNBT(NBTTagCompound par1NBTTagCompound)
	{
		super.readEntityFromNBT(par1NBTTagCompound);
		this.setSheared(par1NBTTagCompound.getBoolean("Sheared"));
		this.setFleeceColor(par1NBTTagCompound.getByte("Color"));
	}
	@Override
	protected String getLivingSound()
	{
		return "mob.sheep.say";
	}
	@Override
	protected String getHurtSound()
	{
		return "mob.sheep.say";
	}
	@Override
	protected String getDeathSound()
	{
		return "mob.sheep.say";
	}

	@Override
	public int getFleeceColor()
	{
		return this.dataWatcher.getWatchableObjectByte(16) & 15;
	}

	@Override
	public void setFleeceColor(int par1)
	{
		byte var2 = this.dataWatcher.getWatchableObjectByte(16);
		this.dataWatcher.updateObject(16, Byte.valueOf((byte)(var2 & 240 | par1 & 15)));
	}

	/**
	 * returns true if a sheeps wool has been sheared
	 */
	@Override
	public boolean getSheared()
	{
		return (this.dataWatcher.getWatchableObjectByte(16) & 16) != 0;
	}

	/**
	 * make a sheep sheared if set to true
	 */
	@Override
	public void setSheared(boolean par1)
	{
		byte var2 = this.dataWatcher.getWatchableObjectByte(16);

		if (par1)
		{
			this.dataWatcher.updateObject(16, Byte.valueOf((byte)(var2 | 16)));
		}
		else
		{
			this.dataWatcher.updateObject(16, Byte.valueOf((byte)(var2 & -17)));
		}
	}

	/**
	 * This method is called when a sheep spawns in the world to select the color of sheep fleece.
	 */
	public static int getRandomFleeceColor(Random par0Random)
	{
		int var1 = par0Random.nextInt(100);
		return var1 < 5 ? 15 : (var1 < 10 ? 7 : (var1 < 15 ? 8 : (var1 < 18 ? 12 : (par0Random.nextInt(500) == 0 ? 6 : 0))));
	}

	/**
	 * This function is used when two same-species animals in 'love mode' breed to generate the new baby animal.
	 */
	/*@Override
	public void procreate(EntityAnimal par1EntityAnimal)
	{
		EntitySheepTFC var2 = (EntitySheepTFC)par1EntityAnimal;
		EntitySheepTFC var3 = new EntitySheepTFC(this.worldObj);

		if (this.rand.nextBoolean())
		{
			var3.setFleeceColor(this.getFleeceColor());
		}
		else
		{
			var3.setFleeceColor(var2.getFleeceColor());
		}

		worldObj.spawnEntityInWorld(var3);
	}*/

	/**
	 * This function applies the benefits of growing back wool and faster growing up to the acting entity. (This
	 * function is used in the AIEatGrass)
	 */
	@Override
	public void eatGrassBonus()
	{
		super.eatGrassBonus();
		this.setSheared(false);
	}

	@Override
	public boolean isShearable(ItemStack item, World world, int X, int Y, int Z) 
	{
		return !getSheared() && isAdult();
	}

	@Override
	public ArrayList<ItemStack> onSheared(ItemStack item, World world, int X, int Y, int Z, int fortune) 
	{
		ArrayList<ItemStack> ret = new ArrayList<ItemStack>();
		setSheared(true);        
		ret.add(new ItemStack(TFCItems.Wool));

		return ret;
	}

	@Override
	public GenderEnum getGender() 
	{
		return GenderEnum.genders[this.getEntityData().getInteger("Sex")];
	}

	@Override
	public EntityAgeable createChild(EntityAgeable entityageable) 
	{
		return new EntityChickenTFC(worldObj, this);
	}

	@Override
	public int getAge() 
	{
		return this.dataWatcher.getWatchableObjectInt(12);
	}

	@Override
	public int getNumberOfDaysToAdult() 
	{
		return TFC_Time.daysInMonth * 3;
	}

	@Override
	public boolean isAdult() 
	{
		return getAge() >= getNumberOfDaysToAdult();
	}

	@Override
	public float getSize() 
	{
		return 0.5f;
	}

	@Override
	public boolean isPregnant() 
	{
		return pregnant;
	}

	@Override
	public EntityLiving getEntity() 
	{
		return this;
	}

	@Override
	public boolean canMateWith(IAnimal animal) 
	{
		if(animal.getGender() != this.getGender() && animal.isAdult() && animal instanceof EntityChickenTFC) {
			return true;
		} else {
			return false;
		}
	}

	@Override
	public void mate(IAnimal otherAnimal) 
	{
		if (sex == 0)
		{
			otherAnimal.mate(this);
			return;
		}
		conception = TFC_Time.getTotalTicks();
		pregnant = true;
		//targetMate.setGrowingAge (TFC_Settings.dayLength);
		resetInLove();
		otherAnimal.setInLove(false);
		mateSizeMod = otherAnimal.getSize();
	}

	@Override
	public void setInLove(boolean b) 
	{
		this.inLove = b;
	}

	@Override
	public long getAnimalID() 
	{
		return animalID;
	}

	@Override
	public void setAnimalID(long id) 
	{
		animalID = id;
	}

	@Override
	public int getHunger() {
		return hunger;
	}

	@Override
	public void setHunger(int h) 
	{
		hunger = h;
	}
}
