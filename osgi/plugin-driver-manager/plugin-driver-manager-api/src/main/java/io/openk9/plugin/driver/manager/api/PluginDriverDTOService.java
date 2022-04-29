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

package io.openk9.plugin.driver.manager.api;

import io.openk9.auth.api.UserInfo;
import io.openk9.plugin.driver.manager.model.PluginDriverContextDTO;
import io.openk9.plugin.driver.manager.model.PluginDriverDTO;
import io.openk9.plugin.driver.manager.model.PluginDriverDTOList;

import java.util.Collection;
import java.util.Optional;

public interface PluginDriverDTOService {

	Optional<PluginDriverDTO> findPluginDriverDTOByName(String name);

	PluginDriverDTOList findPluginDriverDTOByNames(Collection<String> names);

	PluginDriverContextDTO findPluginDriverContextDTO(
		Collection<String> names, UserInfo userInfo);

	PluginDriverDTOList findPluginDriverDTOList();

}
