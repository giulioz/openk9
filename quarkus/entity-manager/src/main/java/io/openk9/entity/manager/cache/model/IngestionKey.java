package io.openk9.entity.manager.cache.model;


import com.hazelcast.nio.ObjectDataInput;
import com.hazelcast.nio.ObjectDataOutput;
import com.hazelcast.nio.serialization.IdentifiedDataSerializable;
import com.hazelcast.partition.PartitionAware;
import io.openk9.entity.manager.cache.EntityManagerDataSerializableFactory;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.NoArgsConstructor;

import java.io.IOException;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor(staticName = "of")
@EqualsAndHashCode
public class IngestionKey implements IdentifiedDataSerializable, PartitionAware<String> {
	private long entityId;
	private String ingestionId;
	private long tenantId;

	@Override
	public String getPartitionKey() {
		return ingestionId;
	}

	@Override
	public int getFactoryId() {
		return EntityManagerDataSerializableFactory.FACTORY_ID;
	}

	@Override
	public int getClassId() {
		return EntityManagerDataSerializableFactory.INGESTION_KEY_TYPE;
	}

	@Override
	public void writeData(ObjectDataOutput out) throws IOException {
		out.writeLong(entityId);
		out.writeString(ingestionId);
		out.writeLong(tenantId);
	}

	@Override
	public void readData(ObjectDataInput in) throws IOException {
		entityId = in.readLong();
		ingestionId = in.readString();
		tenantId = in.readLong();
	}
}
