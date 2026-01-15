<script>
  import Theme from "./Theme.svelte";
</script>

<div class="space-y-6">
  <div>
    <h3 class="text-lg font-medium">Appearance</h3>
    <p class="text-muted-foreground text-sm">
      Customize the appearance of the app. Automatically switch
      between day and night themes.
    </p>
  </div>
  <div class="border-border border-t"></div>
  <div class="space-y-2">
    <label class="uk-form-label block" for="email">Font Family</label>
    <div class="h-10">
      <uk-select
        icon="chevrons-up-down"
        class="fr-select"
        cls-custom="button: uk-input-fake justify-between; dropdown: w-full"
        name="email"
        id="email"
      >
        <select hidden>
          <optgroup label="Select a font family">
            <option>Inter</option>
            <option selected>Geist</option>
            <option>Open Sans</option>
          </optgroup>
        </select>
      </uk-select>
    </div>
    <div class="uk-form-help text-muted-foreground">
      Set the font you want to use in the dashboard.
    </div>
  </div>
  <div class="space-y-2">
    <span class="uk-form-label">Theme</span>
    <div class="uk-form-help text-muted-foreground">
      Select the theme for the dashboard.
    </div>
    <Theme />
  </div>
</div>
