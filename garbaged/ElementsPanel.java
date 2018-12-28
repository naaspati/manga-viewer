		int index = element.getMangaIndex();

		Point pt = element.getLocationOnScreen();
		int x = pt.x;
		int y = pt.y;

		int posInArray = element.getMangaIndex();
		int index = 0;

		for (int i : mod) {
			if(posInArray == i) break;
			else index++;
		}

		ViewElement element2 = null;
		//TODO roller will be implemented here

		if (key == VK_DOWN) {

			if (y >= content.getByMangaIndex(mod.last()).getLocationOnScreen().y)
				element2 = content.getByMangaIndex(mod.first());
			else {
				index++;
				for (; index < mod.length(); index++) {
					element2 = content.getByMangaIndex(mod.at(index));

					if (element2.getLocationOnScreen().x >= x && element2.getLocationOnScreen().y > y)
						break;
				}
			}
		} else if (key == VK_UP) {
			if (y <= content.getByMangaIndex(mod.first()).getLocationOnScreen().y)
				element2 = content.getByMangaIndex(mod.last());
			else {
				index--;
				for (; index > -1; index--) {
					element2 = content.getByMangaIndex(mod.at(index));
					if (element2.getLocationOnScreen().x <= x && element2.getLocationOnScreen().y < y)
						break;
				}
			}
		}

		if(element2 != null)
			element2.requestFocus();