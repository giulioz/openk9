package io.openk9.entity.manager.cache.model;

import com.hazelcast.nio.ObjectDataInput;
import com.hazelcast.nio.ObjectDataOutput;
import com.hazelcast.nio.serialization.IdentifiedDataSerializable;
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
@AllArgsConstructor
@EqualsAndHashCode(onlyExplicitlyIncluded = true)
public class EntityWithRelation implements IdentifiedDataSerializable {
	private Long id;
	@EqualsAndHashCode.Include
	private Long cacheId;
	private Long tenantId;
	private Long tmpId;
	private String ingestionId;
	private String name;
	private String type;

	@Override
	public int getFactoryId() {
		return EntityManagerDataSerializableFactory.FACTORY_ID;
	}

	@Override
	public int getClassId() {
		return EntityManagerDataSerializableFactory.ENTITY_WITH_RELATION_TYPE;
	}

	@Override
	public void writeData(ObjectDataOutput out) throws IOException {
		out.writeObject(id);
		out.writeObject(cacheId);
		out.writeObject(tenantId);
		out.writeObject(tmpId);
		out.writeString(ingestionId);
		out.writeString(name);
		out.writeString(type);
	}

	@Override
	public void readData(ObjectDataInput in) throws IOException {
		id = in.readObject();
		cacheId = in.readObject();
		tenantId = in.readObject();
		tmpId = in.readObject();
		ingestionId = in.readString();
		name = in.readString();
		type = in.readString();
	}
}
