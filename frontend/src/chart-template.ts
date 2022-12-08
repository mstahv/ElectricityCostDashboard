import {css, html, LitElement} from 'lit';
import {customElement, property} from 'lit/decorators.js';
import '@vaadin/charts';
import '@vaadin/charts/src/vaadin-chart-series';

@customElement('chart-template')
export class ChartTemplate extends LitElement {

  static styles = css`
  `;

  @property()
  chartTitle?: string = '';

  @property()
  subtitle?: string = '';

  @property()
  seriesTitle?: string = '';

  @property()
  unit?: string = '';

  @property()
  postfix?: string = 'c/kWh';

  @property()
  values?: Array<number>;

  render() {
    return html`
      <vaadin-chart
          style="height:100%"
          title="${this.chartTitle}"
          subtitle="${this.subtitle}"
          tooltip
          type="column"
          categories='["0:00","1:00","2:00", "3:00", "4:00", "5:00", "6:00", "7:00", "8:00", "9:00", "10:00", "11:00", "12:00", "13:00", "14:00", "15:00", "16:00", "17:00", "18:00", "19:00", "20:00", "21:00", "22:00", "23:00"]'
          additional-options='{
              //"legend": {
              //    layout": "vertical",
              //    "align": "right",
              //    "verticalAlign": "middle"
              //},
              "xAxis": {
                  "crosshair": true
              },
              "animation": false
              //"yAxis": {
              //  "min": -10
              //}
            }'


      >
        <vaadin-chart-series
            title="${this.seriesTitle}"
            unit="${this.unit}"
            .values="${this.values}"
            additional-options='{
                "tooltip": {
                    "pointFormat": "{point.y} ${this.postfix}",
                    "valueDecimals": 2
                },
                "animation": false
            }'
        ></vaadin-chart-series>
      </vaadin-chart>
    `;
  }

}