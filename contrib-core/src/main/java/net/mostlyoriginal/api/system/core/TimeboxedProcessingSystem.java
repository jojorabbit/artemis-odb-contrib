package net.mostlyoriginal.api.system.core;


import com.artemis.Aspect;
import com.artemis.BaseEntitySystem;
import com.artemis.World;
import com.artemis.utils.IntBag;


/**
 * Entity system with limited time allotment.
 *
 * Processes entities for the allotted time. Stops processing
 * entities when the allotted time has passed and will continue
 * where it left off.
 *
 * Since entity processing takes an unpredictable amount of
 * time the system can go one {@see #process} over the allotment.
 *
 * Will never process an entity twice in one cycle.
 * Should be unaffected by deleted entities.
 *
 * Unaffected by Delta. Override {@see #getAllottedTime()} to
 * create a varied allotment.
 *
 * @author Daan van Yperen
 * @author Adrian Papari
 */
public abstract class TimeboxedProcessingSystem extends BaseEntitySystem {

	public static final float MILLISECONDS_PER_SECOND = 1000;

	/** Last index processed */
	public int index;
	private int lastProcessedEntityId =-1;
	private int processedEntities;

	/**
	 * Creates a new TimeboxedProcessingSystem.
	 *
	 * @param aspect
	 *			the aspect to match entities
	 */
	public TimeboxedProcessingSystem(Aspect.Builder aspect) {
		super(aspect);
	}

	@Override
	protected void setWorld(World world) {
		super.setWorld(world);
	}

	/**
	 * Process a entity this system is interested in.
	 *
	 * @param entityId
	 *			the entity to process
	 */
	protected abstract void process(int entityId);

	/**
	 * @return alloted time in seconds.
	 */
	protected abstract float getAllottedTime();

	/**
	 * Iterate over all entities.
	 *
	 * Stop when allotted time has passed.
	 * Stop when all entities have been cycled.
	 */
	@Override
	protected final void processSystem() {

		final IntBag actives = subscription.getEntities();
		final int[] array = actives.getData();

		final int size = actives.size();
		int processed = 0;
		if ( size > 0 ) {

			long time = getTime();
			final long deadline = time + (long) (getAllottedTime() * MILLISECONDS_PER_SECOND);

			index = index % size; // avoid breakage upon subscription changes.
			while ((processed < size) && (time < deadline)) {
				lastProcessedEntityId = array[index];
				process(lastProcessedEntityId);
				index = ++index % size;
				processed++;
				time = getTime();
			}
		}
		processedEntities = processed;
	}

	@Override
	protected void removed(int id) {

		// because subscriptions are always sorted by id,
		// we can assume smaller ids mean the index has shifted.
		if ( (id <= lastProcessedEntityId) && index > 0 ) {
			// re-align the index.
			index--;
		}
	}

	/**
	 * Determine elapsed time.
	 *
	 * @return Current time in milliseconds.
	 */
	protected long getTime() {
		return System.currentTimeMillis();
	}

	/**
	 * @return number of entities processed last {@see #processSystem}.
	 */
	public int getProcessedEntities() {
		return processedEntities;
	}
}
