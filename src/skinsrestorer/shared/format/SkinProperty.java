/**
 * This program is free software; you can redistribute it and/or
 * modify it under the terms of the GNU General Public License
 * as published by the Free Software Foundation; either version 3
 * of the License, or (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program; if not, write to the Free Software
 * Foundation, Inc., 51 Franklin Street, Fifth Floor, Boston, MA 02110-1301, USA.
 *
 */

package skinsrestorer.shared.format;

public class SkinProperty implements Cloneable {

	private String name;
	private String value;
	private String signature;

	public SkinProperty(String name, String value, String signature) {
		this.name = name;
		this.value = value;
		this.signature = signature;
	}

	public String getName() {
		return name;
	}

	public String getValue() {
		return value;
	}

	public boolean hasSignature() {
		return signature != null;
	}

	public String getSignature() {
		return signature;
	}

	@Override
	public SkinProperty clone() {
		return new SkinProperty(new String(name.toCharArray()), new String(value.toCharArray()), new String(signature.toCharArray()));
	}

}
