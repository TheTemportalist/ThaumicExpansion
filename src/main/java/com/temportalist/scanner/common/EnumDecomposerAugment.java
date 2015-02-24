package com.temportalist.scanner.common;

/**
 * @author TheTemportalist
 */
public enum EnumDecomposerAugment {

	// auto ejection
	INTEGRATED_SERVO(0),
	// redstone control
	INTEGRATED_REDSTONE(32),
	SECONDARY_RECEPTION_COIL(128, 2, 3, new double[] { .95, 1, 1, 1 }),
	OVERCLOCK_GEARBOX(129, 4, 8, new double[] { .85, 1, 1, 1 }),
	SPACE_TIME_FLUX(130, 8, 20, new double[] { .70, 1, 1, 1 }),
	// keeps items in, and can only hold 1 at a time
	ITEM_KEEPER(Scanner.MODID + ":augment-itemKeeper:0"),
	THAUMIC_ADJUSTER(Scanner.MODID + ":augment-thaulmicAdjuster:0", 1, 1,
			new double[] { 1, 1, 1, 1.1 });

	private final String stackName;

	private final double speedMultiplier, energyRequirementMultiplier;
	private final double[] outputMultipliers;

	private EnumDecomposerAugment(String itemFullName, double speedMult,
			double energyRequiredMult, double[] outputMults) {
		this.stackName = itemFullName;
		this.speedMultiplier = speedMult;
		this.energyRequirementMultiplier = energyRequiredMult;
		this.outputMultipliers = outputMults;
	}

	private EnumDecomposerAugment(String itemFullName) {
		this(itemFullName, 1, 1, new double[] { 1, 1, 1, 1 });
	}

	private EnumDecomposerAugment(int temeta, double speedMultiplier,
			double energyRequirementMultiplier, double[] outputMultipliers) {
		this("ThermalExpansion:augment:" + temeta,
				speedMultiplier, energyRequirementMultiplier, outputMultipliers);
	}

	private EnumDecomposerAugment(int temeta) {
		this(temeta, 1, 1, new double[] { 1, 1, 1, 1 });
	}

	public String getStackName() {
		return this.stackName;
	}

	public double getSpeedMultiplier() {
		return speedMultiplier;
	}

	public double getTimeMultiplier() {
		return 1d / this.getSpeedMultiplier();
	}

	public double getEnergyRequirementMultiplier() {
		return energyRequirementMultiplier;
	}

	public double[] getOutputMultipliers() {
		return outputMultipliers;
	}

	public static EnumDecomposerAugment getByName(String stackName) {
		for (EnumDecomposerAugment enumType : EnumDecomposerAugment.values()) {
			if (enumType.getStackName().equals(stackName))
				return enumType;
		}
		return null;
	}

}
