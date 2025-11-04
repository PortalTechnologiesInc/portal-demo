

<script>

</script>


<a class="uk-btn uk-btn-default" href="#theme-switcher-modal" data-uk-toggle>
  Theme
</a>

<div class="uk-modal" id="theme-switcher-modal" data-uk-modal>
  <div class="uk-modal-dialog">
    <button
      class="uk-modal-close absolute right-4 top-4"
      type="button"
      data-uk-close
    ></button>
    <div class="uk-modal-header">
      <div class="uk-modal-title">Customize</div>
    </div>
    <div class="uk-modal-body">
      <uk-theme-switcher id="theme-switcher">
        <select hidden>
          <optgroup data-key="theme" label="Theme">
            <option data-hex="#52525b" value="uk-theme-zinc">Zinc</option>

            <option data-hex="#64748b" value="uk-theme-slate">Slate</option>
            <option data-hex="#78716c" value="uk-theme-stone">Stone</option>
            <option data-hex="#6b7280" value="uk-theme-gray">Gray</option>
            <option data-hex="#737373" value="uk-theme-neutral" selected>
              Neutral
            </option>
            <option data-hex="#dc2626" value="uk-theme-red">Red</option>
            <option data-hex="#e11d48" value="uk-theme-rose">Rose</option>
            <option data-hex="#f97316" value="uk-theme-orange">Orange</option>
            <option data-hex="#65a30d" value="uk-theme-green">Green</option>
            <option data-hex="#2563eb" value="uk-theme-blue">Blue</option>
            <option data-hex="#facc15" value="uk-theme-yellow">Yellow</option>
            <option data-hex="#7c3aed" value="uk-theme-violet">Violet</option>
            <option data-hex="#d97706" value="uk-theme-amber">Amber</option>
            <option data-hex="#9333ea" value="uk-theme-purple">Purple</option>
            <option data-hex="#0d9488" value="uk-theme-teal">Teal</option>
          </optgroup>
          <optgroup data-key="radii" label="Radii">
            <option value="uk-radii-none">None</option>
            <option value="uk-radii-sm">Small</option>
            <option value="uk-radii-md" selected>Medium</option>
            <option value="uk-radii-lg">Large</option>
          </optgroup>
          <optgroup data-key="shadows" label="Shadows">
            <option value="uk-shadows-none">None</option>
            <option value="uk-shadows-sm" selected>Small</option>
            <option value="uk-shadows-md">Medium</option>
            <option value="uk-shadows-lg">Large</option>
          </optgroup>
          <optgroup data-key="font" label="Font">
            <option value="uk-font-sm" selected>Small</option>
            <option value="uk-font-base">Default</option>
          </optgroup>
          <optgroup data-key="mode" label="Mode">
            <option data-icon="sun" value="light">Light</option>
            <option data-icon="moon" value="dark">Dark</option>
          </optgroup>
        </select>
      </uk-theme-switcher>
    </div>
  </div>
</div>

