/*
 * Copyright (c) 2020-present SMC Treviso s.r.l. All rights reserved.
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU Affero General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU Affero General Public License for more details.
 *
 * You should have received a copy of the GNU Affero General Public License
 * along with this program.  If not, see <http://www.gnu.org/licenses/>.
 */

package io.openk9.datasource.event.sender;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

public interface EventSender {

	void sendEventAsJson(String type, Object data);

	void sendEventAsJson(String type, String groupKey, Object data);

	void sendEventAsJson(
		String type, String groupKey, String className, Object data);

	@Data
	@NoArgsConstructor
	@AllArgsConstructor(staticName = "of")
	@Builder
	class EventMessage {
		private String type;
		private String groupKey;
		private String className;
		private Object data;
	}
}
